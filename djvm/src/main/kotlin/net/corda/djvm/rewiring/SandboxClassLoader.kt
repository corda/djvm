package net.corda.djvm.rewiring

import net.corda.djvm.SandboxConfiguration
import net.corda.djvm.TypedTaskFactory
import net.corda.djvm.analysis.AnalysisConfiguration
import net.corda.djvm.analysis.AnalysisContext
import net.corda.djvm.analysis.ClassAndMemberVisitor
import net.corda.djvm.analysis.SyntheticResolver.Companion.getDJVMSyntheticOwner
import net.corda.djvm.analysis.SyntheticResolver.Companion.isDJVMSynthetic
import net.corda.djvm.code.asPackagePath
import net.corda.djvm.code.asResourcePath
import net.corda.djvm.execution.SandboxRuntimeException
import net.corda.djvm.references.ClassReference
import net.corda.djvm.source.ClassSource
import net.corda.djvm.source.CodeLocation
import net.corda.djvm.source.SourceClassLoader
import net.corda.djvm.utilities.loggerFor
import net.corda.djvm.validation.RuleValidator
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassReader.SKIP_FRAMES
import java.io.IOException
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.net.MalformedURLException
import java.net.URL
import java.security.AccessController.doPrivileged
import java.security.CodeSource
import java.security.PrivilegedAction
import java.security.PrivilegedActionException
import java.security.PrivilegedExceptionAction
import java.security.SecureClassLoader
import java.util.Enumeration
import java.util.LinkedList
import java.util.concurrent.ConcurrentMap
import java.util.function.Function

/**
 * Class loader that enables registration of rewired classes.
 *
 * @property analysisConfiguration The configuration to use for the analysis.
 * @property analyzer The instance used to validate that any loaded class complies with the specified rules.
 * @property supportingClassLoader The class loader used to find classes on the extended class path.
 * @property rewriter The re-writer to use for registered classes.
 * @property context The context in which analysis and processing is performed.
 * @property byteCodeCache Precomputed class bytecode, to save us from regenerating it.
 * @property externalCache An externally-provided [ConcurrentMap] of pre-computed byte-code.
 * @param throwableClass This sandbox's definition of [sandbox.java.lang.Throwable].
 * @param parent This classloader's parent classloader.
 */
class SandboxClassLoader private constructor(
    private val analysisConfiguration: AnalysisConfiguration,
    private val analyzer: ClassAndMemberVisitor,
    private val supportingClassLoader: SourceClassLoader,
    private val rewriter: ClassRewriter,
    private val context: AnalysisContext,
    private val byteCodeCache: ByteCodeCache,
    private val externalCache: ExternalCache?,
    throwableClass: Class<*>?,
    annotationClass: Class<*>?,
    parent: ClassLoader?
) : SecureClassLoader(parent ?: SandboxClassLoader::class.java.classLoader), AutoCloseable {

    /**
     * Set of classes that should be left untouched due to whitelisting.
     */
    private val whitelistedClasses = analysisConfiguration.whitelist

    /**
     * Cache of loaded byte-code.
     */
    private val loadedByteCode = mutableMapOf<String, ByteCode>()
    private val codeLocations: MutableMap<String, CodeLocation>
                    = supportingClassLoader.codeLocations.associateByTo(LinkedHashMap(), CodeLocation::location)

    /**
     * Update the common byte-code cache with the classes we have generated.
     */
    override fun close() {
        System.getSecurityManager()?.apply {
            checkPermission(RuntimePermission("closeClassLoader"))
        }
        (parent as? SandboxClassLoader)?.close()
        byteCodeCache.update(loadedByteCode)
    }

    /**
     * We need to load [sandbox.java.lang.Throwable] up front, so that we can
     * identify sandboxed exception classes.
     */
    private val throwableClass: Class<*> = throwableClass ?: run {
        loadClassAndBytes(ClassSource.fromClassName("sandbox.java.lang.Object"), context)
        loadClassAndBytes(ClassSource.fromClassName("sandbox.java.lang.StackTraceElement"), context)
        loadClassAndBytes(ClassSource.fromClassName("sandbox.java.lang.Throwable"), context)
    }

    /**
     * We also need to load [sandbox.java.lang.annotation.Annotation] up front,
     * so that we can identify sandboxed annotation classes.
     */
    private val annotationClass: Class<*> = annotationClass ?: run {
        loadClassAndBytes(ClassSource.fromClassName("sandbox.java.lang.annotation.Annotation"), context)
    }

    /**
     * Enables / disables the external cache for this [SandboxClassLoader].
     *
     * IMPORTANT! Declaring this property after [throwableClass] means that
     * its value will always be false while this basic class is pre-loaded.
     */
    var externalCaching: Boolean = (externalCache != null)
        set(value) {
            field = value && (externalCache != null)
        }

    /**
     * Creates an empty [SandboxClassLoader] with exactly the same
     * configuration as this one, but with the given [AnalysisContext].
     * @param newContext The [AnalysisContext] to use for the child classloader.
     */
    fun copyEmpty(newContext: AnalysisContext) = SandboxClassLoader(
        analysisConfiguration,
        analyzer,
        supportingClassLoader,
        rewriter,
        newContext,
        byteCodeCache,
        externalCache,
        throwableClass,
        annotationClass,
        parent
    )

    /**
     * Returns an instance of [Function] that can transform a
     * basic Java object into its equivalent inside the sandbox.
     */
    @Throws(
        ClassNotFoundException::class,
        IllegalAccessException::class,
        InstantiationException::class,
        InvocationTargetException::class,
        NoSuchMethodException::class
    )
    fun createBasicInput(): Function<in Any?, out Any?> {
        return createBasicTask("sandbox.BasicInput")
    }

    /**
     * Returns an instance of [Function] that can transform
     * a basic sandbox object into its equivalent Java object.
     */
    @Throws(
        ClassNotFoundException::class,
        IllegalAccessException::class,
        InstantiationException::class,
        InvocationTargetException::class,
        NoSuchMethodException::class
    )
    fun createBasicOutput(): Function<in Any?, out Any?> {
        return createBasicTask("sandbox.BasicOutput")
    }

    @Throws(
        ClassNotFoundException::class,
        IllegalAccessException::class,
        InstantiationException::class,
        InvocationTargetException::class,
        NoSuchMethodException::class
    )
    private fun createBasicTask(taskName: String): Function<in Any?, out Any?> {
        val taskClass = loadClass(taskName)
        @Suppress("unchecked_cast")
        val task = try {
            doPrivileged(PrivilegedExceptionAction { taskClass.getDeclaredConstructor() })
        } catch (e: PrivilegedActionException) {
            throw e.cause ?: e
        }.newInstance() as Function<in Any?, out Any?>
        return Function { value ->
            try {
                task.apply(value)
            } catch (target: Throwable) {
                throw when (target) {
                    is RuntimeException, is Error -> target
                    else -> SandboxRuntimeException(target.message, target)
                }
            }
        }
    }

    /**
     * Returns an instance of [Function] that can wrap an
     * instance of [sandbox.java.util.function.Function].
     * The function's input and output are marshalled using
     * the [sandbox.BasicInput] and [sandbox.BasicOutput]
     * transformations.
     */
    @Throws(
        ClassNotFoundException::class,
        NoSuchMethodException::class
    )
    fun createTaskFactory(): Function<in Any, out Function<in Any?, out Any?>> {
        return createTaskFactory("sandbox.Task")
    }

    /**
     * Returns an instance of [Function] that can wrap an
     * instance of [sandbox.java.util.function.Function].
     * The function's input and output are not marshalled.
     */
    @Throws(
        ClassNotFoundException::class,
        NoSuchMethodException::class
    )
    fun createRawTaskFactory(): Function<in Any, out Function<in Any?, out Any?>> {
        return createTaskFactory("sandbox.RawTask")
    }

    /**
     * Factory to create a [Function] that will execute a sandboxed
     * task that implements [sandbox.java.util.function.Function].
     * This is just a convenience function which assumes that the
     * task has a no-argument constructor, but is still likely to
     * be what you want.
     * The task's input and output are marshalled using
     * the [sandbox.BasicInput] and [sandbox.BasicOutput]
     * transformations.
     */
    @Throws(
        ClassNotFoundException::class,
        NoSuchMethodException::class
    )
    fun createTypedTaskFactory(): TypedTaskFactory {
        val typedTaskFactory = createTaskFactory().compose(createSandboxFunction())
        return object : TypedTaskFactory {
            override fun <T, R> create(taskClass: Class<out Function<T, R>>): Function<T, R> {
                @Suppress("unchecked_cast")
                return typedTaskFactory.apply(taskClass) as Function<T, R>
            }
        }
    }

    @Throws(
        ClassNotFoundException::class,
        NoSuchMethodException::class
    )
    private fun createTaskFactory(taskName: String): Function<in Any, out Function<in Any?, out Any?>> {
        val taskClass = loadClass(taskName)
        val functionClass = loadClass("sandbox.java.util.function.Function")
        val constructor = try {
            doPrivileged(PrivilegedExceptionAction {
                @Suppress("unchecked_cast")
                taskClass.getDeclaredConstructor(functionClass) as Constructor<out Function<in Any?, out Any?>>
            })
        } catch (e: PrivilegedActionException) {
            throw e.cause ?: e
        }
        return Function { userTask ->
            try {
                constructor.newInstance(userTask)
            } catch (ex: Throwable) {
                val target = (ex as? InvocationTargetException)?.cause ?: ex
                throw when (target) {
                    is RuntimeException, is Error -> target
                    else -> SandboxRuntimeException(target.message, target)
                }
            }
        }
    }

    /**
     * Factory to create a [Function] that will execute a sandboxed
     * instance of a task, where this task also implements [Function].
     * This is just a convenience function which assumes that the
     * task has a no-argument constructor, but is still likely to be
     * what you want.
     */
    fun createSandboxFunction(): Function<Class<out Function<*, *>>, out Any> {
        return Function { taskClass ->
            try {
                val sandboxClass = toSandboxClass(taskClass)
                doPrivileged(PrivilegedExceptionAction { sandboxClass.getDeclaredConstructor() }).newInstance()
            } catch (ex: Throwable) {
                val target = when(ex) {
                    is PrivilegedActionException, is InvocationTargetException -> ex.cause ?: ex
                    else -> ex
                }
                throw when (target) {
                    is RuntimeException, is Error -> target
                    else -> SandboxRuntimeException(target.message, target)
                }
            }
        }
    }

    /**
     * Wraps an instance of [Function] inside a task that implements
     * both [Function] and [sandbox.java.util.function.Function].
     */
    @Throws(
        ClassNotFoundException::class,
        IllegalAccessException::class,
        InstantiationException::class,
        InvocationTargetException::class,
        NoSuchMethodException::class
    )
    fun <T> createForImport(task: Function<in T, out Any?>): Function<in T, out Any?> {
        val taskClass = loadClass("sandbox.ImportTask")
        @Suppress("unchecked_cast")
        return try {
            doPrivileged(PrivilegedExceptionAction { taskClass.getDeclaredConstructor(Function::class.java) })
        } catch (e: PrivilegedActionException) {
            throw e.cause ?: e
        }.newInstance(task) as Function<in T, out Any?>
    }

    /**
     * Given a class name, provide its corresponding [LoadedClass] for the sandbox.
     * This class may have been loaded by a parent classloader really.
     */
    @Throws(ClassNotFoundException::class)
    fun loadForSandbox(className: String): LoadedClass {
        val sandboxClass = loadClassForSandbox(className)
        val sandboxName = sandboxClass.name
        var loader = this
        while(true) {
            val byteCode = loader.loadedByteCode[sandboxName]
            if (byteCode != null) {
                return LoadedClass(sandboxClass, byteCode)
            }
            loader = loader.parent as? SandboxClassLoader ?: return LoadedClass(sandboxClass, UNMODIFIED)
        }
    }

    @Throws(ClassNotFoundException::class)
    fun loadForSandbox(source: ClassSource): LoadedClass {
        return loadForSandbox(source.qualifiedClassName)
    }

    private fun loadClassForSandbox(className: String): Class<*> {
        return loadClass(resolveName(className))
    }

    @Throws(ClassNotFoundException::class)
    fun loadClassForSandbox(source: ClassSource): Class<*> {
        return loadClassForSandbox(source.qualifiedClassName)
    }

    @Throws(ClassNotFoundException::class)
    fun toSandboxClass(clazz: Class<*>): Class<*> {
        return loadClassForSandbox(ClassSource.fromClassName(clazz.name))
    }

    @Throws(ClassNotFoundException::class)
    fun toSandboxClass(className: String): Class<*> = loadClassForSandbox(className)

    fun resolveName(className: String): String {
        return analysisConfiguration.classResolver.resolveNormalized(className)
    }

    /**
     * Load the class with the specified binary name.
     *
     * @param name The binary name of the class.
     * @param resolve If `true` then resolve the class.
     *
     * @return The resulting <tt>Class</tt> object.
     */
    @Throws(ClassNotFoundException::class)
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        synchronized(getClassLoadingLock(name)) {
            var clazz = findLoadedClass(name)
            if (clazz == null) {
                val source = ClassSource.fromClassName(name)
                val isSandboxClass = analysisConfiguration.isSandboxClass(source.internalClassName)

                // We ALWAYS have a parent classloader, because a sandbox
                // classloader MUST ultimately delegate to the DJVM itself.
                val parentClassLoader = parent
                if (!isSandboxClass || parentClassLoader is SandboxClassLoader) {
                    try {
                        clazz = parentClassLoader.loadClass(name)
                    } catch (_: ClassNotFoundException) {
                    }
                }

                if (clazz == null) {
                    if (isSandboxClass) {
                        clazz = loadSandboxClass(source, context)
                    } else {
                        // We shouldn't reach here, but this function should never return null.
                        throw ClassNotFoundException(name)
                    }
                }
            }
            if (resolve) {
                resolveClass(clazz)
            }
            return clazz
        }
    }

    /**
     * A sandboxed exception class cannot be thrown, and so we may also need to create a
     * synthetic throwable wrapper for it. Or perhaps we've just been asked to load the
     * synthetic wrapper class belonging to an exception that we haven't loaded yet?
     * Either way, we need to load the sandboxed exception first so that we know what
     * the synthetic wrapper's super-class needs to be.
     *
     * The same logic also applies to annotation classes because these must extend
     * [java.lang.annotation.Annotation] in order to annotate anything. So we create
     * synthetic annotations to store the values in the byte-code, and translate
     * them to "annotation-like" equivalent interfaces inside the sandbox.
     */
    private fun loadSandboxClass(source: ClassSource, context: AnalysisContext): Class<*> {
        return if (isDJVMSynthetic(source.internalClassName)) {
            /**
             * We need to load the owner class before we can create its synthetic friend class.
             * And loading the owner should then also create the synthetic class automatically.
             */
            val syntheticOwner = ClassSource.fromClassName(getDJVMSyntheticOwner(source.qualifiedClassName))
            if (analysisConfiguration.hasDJVMSynthetic(syntheticOwner.internalClassName)) {
                /**
                 * We must not create a duplicate synthetic wrapper inside any child
                 * classloader. Hence we re-invoke [loadClass] which will delegate
                 * back to the parent.
                 *
                 * JVM Exceptions belong to the bootstrap classloader, and so will
                 * never be found inside a child classloader.
                 */
                loadClass(syntheticOwner.qualifiedClassName, false)
            }
            findLoadedClass(source.qualifiedClassName) ?: throw ClassNotFoundException(source.qualifiedClassName)
        } else {
            loadClassAndBytes(source, context).also { clazz ->
                when {
                    /**
                     * Check whether we've just loaded a sandboxed throwable class.
                     * If we have, we may also need to synthesise a throwable wrapper for it.
                     */
                    throwableClass.isAssignableFrom(clazz) ->
                        if (!analysisConfiguration.isJvmException(source.internalClassName)) {
                            logger.debug("Generating synthetic throwable for {}", source.qualifiedClassName)
                            loadWrapperFor(clazz)
                        }

                    /**
                     * Check whether we've just loaded a sandboxed annotation class.
                     * This may also be accompanied by a synthetic friend class.
                     */
                    clazz.isAnnotation && annotationClass.isAssignableFrom(clazz) ->
                        if (!analysisConfiguration.isJvmAnnotation(source.internalClassName)) {
                            logger.debug("Loading synthetic annotation for {}", source.qualifiedClassName)
                            loadAnnotationFor(clazz)
                        }
                }
            }
        }
    }

    /**
     * Load the class with the specified binary name.
     *
     * @param request The class request, including the binary name of the class.
     * @param context The context in which the analysis is conducted.
     *
     * @return The resulting <tt>Class</tt> object and its byte code representation.
     */
    private fun loadClassAndBytes(request: ClassSource, context: AnalysisContext): Class<*> {
        logger.debug("Loading class {}, origin={}...", request.qualifiedClassName, request.origin)
        val requestedPath = request.internalClassName
        val sourceName = analysisConfiguration.classResolver.reverseNormalized(request.qualifiedClassName)
        val resolvedName = analysisConfiguration.classResolver.resolveNormalized(sourceName)

        val byteCode = if (analysisConfiguration.isTemplateClass(requestedPath)) {
            loadUnmodifiedByteCode(requestedPath)
        } else {
            byteCodeCache[request.qualifiedClassName] ?: run {
                // Load the source byte code for the specified class.
                val resourceName = sourceName.asResourcePath + ".class"
                val resource = doPrivileged(PrivilegedAction { supportingClassLoader.getResource(resourceName) })
                        ?: throw ClassNotFoundException("Class file not found: $resourceName")
                val codeLocation = getCodeLocation(resource)

                if (externalCaching && externalCache != null) {
                    val externalKey = ByteCodeKey(
                        request.qualifiedClassName,
                        codeLocation.location
                    )

                    externalCache.getOrPut(externalKey) {
                        generateByteCode(request.qualifiedClassName, resource, codeLocation, context)
                    }
                } else {
                    generateByteCode(request.qualifiedClassName, resource, codeLocation, context)
                }
            }
        }

        // Try to define the transformed class.
        val clazz: Class<*> = try {
            when {
                whitelistedClasses.matches(sourceName.asResourcePath) -> supportingClassLoader.loadClass(sourceName)
                else -> defineClass(resolvedName, byteCode)
            }
        } catch (exception: SecurityException) {
            throw SecurityException("Cannot redefine class '$resolvedName'", exception)
        }

        // Cache transformed byte-code.
        loadedByteCode[request.qualifiedClassName] = byteCode
        if (request.origin != null) {
            context.recordClassOrigin(sourceName, ClassReference(request.origin))
        }

        logger.debug("Loaded class {}, bytes={}, isModified={}",
                request.qualifiedClassName, byteCode.bytes.size, byteCode.isModified)

        return clazz
    }

    private fun getCodeLocation(resource: URL): CodeLocation {
        val location = resource.toLocation()
        return (codeLocations[location] ?: findDirectoryMatch(location)) ?: run {
            /*
             * This is an unlikely event in practice, but the
             * DJVM's own unit tests trigger it when loading
             * the template sandbox classes, e.g. Object.class.
             */
            val sourceUrl = try {
                URL(location)
            } catch (e: MalformedURLException) {
                throw SecurityException(e.message, e)
            }
            val codeLocation = CodeLocation(sourceUrl, location)
            codeLocations[codeLocation.location] = codeLocation
            codeLocation
        }
    }

    private tailrec fun findDirectoryMatch(location: String): CodeLocation? {
        if (!location.endsWith('/')) {
            return null
        }
        var idx = location.length - 2
        while (idx >= 0 && location[idx] != '/') {
            --idx
        }
        val path = location.substring(0, idx + 1)
        return codeLocations[path] ?: findDirectoryMatch(path)
    }

    /**
     * Generates the byte-code for [qualifiedClassName] using byte-code from [resource].
     */
    private fun generateByteCode(qualifiedClassName: String, resource: URL, codeLocation: CodeLocation, context: AnalysisContext): ByteCode {
        return try {
            doPrivileged(PrivilegedExceptionAction {
                val reader = try {
                    resource.openStream().use(::ClassReader)
                } catch (e: IOException) {
                    throw ClassNotFoundException("Error reading source byte-code for $qualifiedClassName: ${e.message}")
                }

                // Analyse the class if not matching the whitelist.
                if (!analysisConfiguration.whitelist.matches(reader.className)) {
                    logger.trace("Class {} does not match with the whitelist", qualifiedClassName)
                    logger.trace("Analyzing class {}...", qualifiedClassName)
                    analyzer.analyze(reader, context, SKIP_FRAMES)
                }

                // Check if any errors were found during analysis.
                if (context.messages.errorCount > 0) {
                    logger.debug("Errors detected after analyzing class {}", qualifiedClassName)
                    throw SandboxClassLoadingException("Analysis failed for $qualifiedClassName", context)
                }

                // Transform the class definition and byte code in accordance with provided rules.
                rewriter.rewrite(reader, codeLocation.codeSource, context).also {
                    if (it.isAnnotation) {
                        logger.debug("Generating synthetic annotation for {}", qualifiedClassName)
                        val annotationName = analysisConfiguration.syntheticResolver.getRealAnnotationName(qualifiedClassName)
                        loadedByteCode[annotationName] = rewriter.generateAnnotation(reader, it.source)
                    }
                }
            })
        } catch (e: PrivilegedActionException) {
            throw e.cause ?: e
        }
    }

    private fun defineClass(name: String, byteCode: ByteCode): Class<*> {
        val idx = name.lastIndexOf('.')
        if (idx > 0) {
            val packageName = name.substring(0, idx)
            /**
             * [getPackage] is deprecated in Java 9 and above.
             */
            @Suppress("deprecation")
            if (getPackage(packageName) == null) {
                definePackage(packageName, null, null, null, null, null, null, null)
            }
        }
        return defineClass(name, byteCode.bytes, 0, byteCode.bytes.size, byteCode.source)
    }

    private fun loadUnmodifiedByteCode(internalClassName: String): ByteCode {
        return try {
            doPrivileged(PrivilegedExceptionAction {
                val resource = getSystemResource("$internalClassName.class")
                    ?: throw ClassNotFoundException(internalClassName)
                val byteStream = try {
                    resource.openStream()
                } catch (_: IOException) {
                    throw ClassNotFoundException(internalClassName)
                }
                ByteCode(
                    bytes = byteStream.use { it.readBytes() },
                    source = getCodeLocation(resource).codeSource
                )
            })
        } catch (e: PrivilegedActionException) {
            throw e.cause ?: e
        }
    }

    /**
     * Check whether the synthetic throwable wrapper already
     * exists for this exception, and create it if it doesn't.
     */
    private fun loadWrapperFor(throwable: Class<*>): Class<*> {
        val className = analysisConfiguration.syntheticResolver.getThrowableName(throwable)
        val loadableClassName = className.asPackagePath
        return findLoadedClass(loadableClassName) ?: run {
            val superName = analysisConfiguration.syntheticResolver.getThrowableSuperName(throwable)
            val byteCode = ThrowableWrapperFactory.toByteCode(className, superName)
            loadedByteCode[loadableClassName] = byteCode
            defineClass(loadableClassName, byteCode)
        }
    }

    private fun loadAnnotationFor(annotation: Class<*>): Class<*> {
        val className = analysisConfiguration.syntheticResolver.getAnnotationName(annotation)
        val loadableClassName = className.asPackagePath
        return findLoadedClass(loadableClassName) ?: run {
            var byteCode = loadedByteCode[loadableClassName]
            if (byteCode != null) {
                /**
                 * The class does not exist yet, but we've still found its
                 * byte-code in the cache. We must therefore have just
                 * generated this byte-code, and so we should also include
                 * it in the external cache, assuming there is one.
                 */
                val codeSource = byteCode.source
                if (codeSource != null && externalCaching && externalCache != null) {
                    val externalKey = createExternalKey(loadableClassName, codeSource)
                    externalCache.putIfAbsent(externalKey, byteCode)
                }
            } else {
                /**
                 * No class and no byte-code either! But we still managed to
                 * create this synthetic class's owner somehow, which means
                 * that class's byte-code must exist in either the internal
                 * or the external cache. And the byte-code we need should
                 * therefore be cached alongside it.
                 */
                byteCode = byteCodeCache[loadableClassName] ?: run {
                    val ownerByteCode = loadedByteCode[annotation.name]
                        ?: throw SandboxClassLoadingException(className, context)
                    val codeSource = ownerByteCode.source
                    if (codeSource != null && externalCaching && externalCache != null) {
                        val externalKey = createExternalKey(loadableClassName, codeSource)
                        externalCache[externalKey]
                    } else {
                        null
                    }
                } ?: throw ClassNotFoundException(className)
                loadedByteCode[loadableClassName] = byteCode
            }
            defineClass(loadableClassName, byteCode)
        }
    }

    private fun createExternalKey(className: String, codeSource: CodeSource): ByteCodeKey {
        return ByteCodeKey(className, codeSource.location.toString().intern())
    }

    /**
     * Forces this [SandboxClassLoader] to load every class that has
     * been referenced while loading the current set of classes.
     * Consumes all of the references queued on the [AnalysisContext].
     * @param knownReferences A set of internal names of classes that
     * we know we have loaded already.
     */
    @Throws(ClassNotFoundException::class)
    fun resolveReferences(knownReferences: MutableSet<String>) {
        val unknownReferences = LinkedList<String>()
        while (true) {
            context.references.process { ref ->
                if (ref is ClassReference && knownReferences.add(ref.className)) {
                    unknownReferences.addLast(ref.className)
                }
            }

            /**
             * No more unknown class references means that all
             * class references from the previous iteration were
             * already known. Which must mean that everything has
             * now been resolved.
             */
            if (unknownReferences.isEmpty()) {
                break
            }

            /**
             * Load the next unknown class, which will
             * in turn generate a new batch of class
             * references to examine.
             */
            toSandboxClass(unknownReferences.removeFirst().asPackagePath)
        }
    }

    /**
     * Allow access to resources in the source classloader.
     */
    override fun getResource(resourceName: String): URL? {
        return supportingClassLoader.getResource(resourceName)
    }

    /**
     * Allow access to resources in the source classloader.
     */
    override fun getResources(resourceName: String): Enumeration<URL> {
        return supportingClassLoader.getResources(resourceName)
    }

    companion object {
        private val logger = loggerFor<SandboxClassLoader>()
        private val UNMODIFIED = ByteCode(ByteArray(0), null)

        private fun URL.toLocation(): String {
            val fullPath = toString()
            return when {
                fullPath.startsWith("jar:") -> fullPath.substring(4, fullPath.indexOf("!/"))
                fullPath.endsWith(".class") -> fullPath.substring(0, fullPath.lastIndexOf('/') + 1)
                else -> fullPath
            }
        }

        /**
         * Factory function to create a [SandboxClassLoader].
         * @param configuration The [SandboxConfiguration] containing the classloader's configuration parameters.
         */
        fun createFor(configuration: SandboxConfiguration): SandboxClassLoader {
            return createFor(configuration, configuration.analysisConfiguration, configuration.byteCodeCache)
        }

        private fun createFor(
            configuration: SandboxConfiguration,
            analysisConfiguration: AnalysisConfiguration,
            byteCodeCache: ByteCodeCache?
        ): SandboxClassLoader {
            val parentClassLoader = analysisConfiguration.parent?.let {
                createFor(configuration, it, byteCodeCache?.parent)
            }
            val supportingClassLoader = analysisConfiguration.supportingClassLoader
            return SandboxClassLoader(
                analysisConfiguration = analysisConfiguration,
                supportingClassLoader = supportingClassLoader,
                analyzer = RuleValidator(rules = configuration.rules,
                                         configuration = analysisConfiguration),
                rewriter = ClassRewriter(configuration, supportingClassLoader),
                context = parentClassLoader?.context ?: AnalysisContext.fromConfiguration(analysisConfiguration),
                byteCodeCache = byteCodeCache ?: ByteCodeCache(parentClassLoader?.byteCodeCache),
                externalCache = configuration.externalCache,
                throwableClass = parentClassLoader?.throwableClass,
                annotationClass = parentClassLoader?.annotationClass,
                parent = parentClassLoader
            )
        }
    }
}
