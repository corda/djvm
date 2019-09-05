package net.corda.djvm.rewiring

import net.corda.djvm.SandboxConfiguration
import net.corda.djvm.analysis.AnalysisConfiguration
import net.corda.djvm.analysis.AnalysisContext
import net.corda.djvm.analysis.ClassAndMemberVisitor
import net.corda.djvm.analysis.ExceptionResolver.Companion.getDJVMExceptionOwner
import net.corda.djvm.analysis.ExceptionResolver.Companion.isDJVMException
import net.corda.djvm.code.asPackagePath
import net.corda.djvm.code.asResourcePath
import net.corda.djvm.execution.SandboxRuntimeException
import net.corda.djvm.references.ClassReference
import net.corda.djvm.source.ClassSource
import net.corda.djvm.source.SourceClassLoader
import net.corda.djvm.utilities.loggerFor
import net.corda.djvm.validation.RuleValidator
import org.objectweb.asm.ClassReader.SKIP_FRAMES
import org.objectweb.asm.Type
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.net.URL
import java.util.function.BiFunction
import java.util.function.Function

/**
 * Class loader that enables registration of rewired classes.
 *
 * @property analysisConfiguration The configuration to use for the analysis.
 * @property ruleValidator The instance used to validate that any loaded class complies with the specified rules.
 * @property supportingClassLoader The class loader used to find classes on the extended class path.
 * @property rewriter The re-writer to use for registered classes.
 * @property context The context in which analysis and processing is performed.
 * @param throwableClass This sandbox's definition of [sandbox.java.lang.Throwable].
 * @param parent This classloader's parent classloader.
 */
class SandboxClassLoader private constructor(
    private val analysisConfiguration: AnalysisConfiguration,
    private val ruleValidator: RuleValidator,
    private val supportingClassLoader: SourceClassLoader,
    private val rewriter: ClassRewriter,
    private val context: AnalysisContext,
    throwableClass: Class<*>?,
    parent: ClassLoader?
) : ClassLoader(parent ?: getSystemClassLoader()) {

    /**
     * The analyzer used to traverse the class hierarchy.
     */
    private val analyzer: ClassAndMemberVisitor
        get() = ruleValidator

    /**
     * Set of classes that should be left untouched due to whitelisting.
     */
    private val whitelistedClasses = analysisConfiguration.whitelist

    /**
     * Cache of loaded classes.
     */
    private val loadedClasses = mutableMapOf<String, LoadedClass>()

    /**
     * We need to load [sandbox.java.lang.Throwable] up front, so that we can
     * identify sandboxed exception classes.
     */
    private val throwableClass: Class<*> = throwableClass ?: run {
        loadClassAndBytes(ClassSource.fromClassName("sandbox.java.lang.Object"), context)
        loadClassAndBytes(ClassSource.fromClassName("sandbox.java.lang.StackTraceElement"), context)
        loadClassAndBytes(ClassSource.fromClassName("sandbox.java.lang.Throwable"), context).type
    }

    /**
     * Creates an empty [SandboxClassLoader] with exactly the same
     * configuration as this one, but with the given [AnalysisContext].
     * @param newContext The [AnalysisContext] to use for the child classloader.
     */
    fun copyEmpty(newContext: AnalysisContext) = SandboxClassLoader(
        analysisConfiguration,
        ruleValidator,
        supportingClassLoader,
        rewriter,
        newContext,
        throwableClass,
        parent
    )

    /**
     * Returns an instance of [Function] that can transform a
     * basic Java object into its equivalent inside the sandbox.
     */
    @Throws(
        ClassNotFoundException::class,
        IllegalAccessException::class,
        InstantiationException::class
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
        InstantiationException::class
    )
    fun createBasicOutput(): Function<in Any?, out Any?> {
        return createBasicTask("sandbox.BasicOutput")
    }

    @Throws(
        ClassNotFoundException::class,
        IllegalAccessException::class,
        InstantiationException::class
    )
    private fun createBasicTask(taskName: String): Function<in Any?, out Any?> {
        val taskClass = loadClass(taskName)
        @Suppress("unchecked_cast")
        val task = taskClass.newInstance() as Function<in Any?, out Any?>
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
     * Returns an instance of [BiFunction] that can execute
     * instances of [sandbox.java.util.function.Function].
     * The function's input and output are marshalled using
     * the [sandbox.BasicInput] and [sandbox.BasicOutput]
     * transformations.
     */
    @Throws(
        ClassNotFoundException::class,
        NoSuchMethodException::class
    )
    fun createExecutor(): BiFunction<in Any, in Any?, out Any?> {
        @Suppress("unchecked_cast")
        return createExecutorTask("sandbox.Task")
    }

    /**
     * Returns an instance of [BiFunction] that can execute
     * instances of [sandbox.java.util.function.Function].
     * The function's input and output are not marshalled.
     */
    @Throws(
        ClassNotFoundException::class,
        NoSuchMethodException::class
    )
    fun createRawExecutor(): BiFunction<in Any, in Any?, out Any?> {
        return createExecutorTask("sandbox.RawTask")
    }

    @Throws(
        ClassNotFoundException::class,
        NoSuchMethodException::class
    )
    private fun createExecutorTask(taskName: String): BiFunction<in Any, in Any?, out Any?> {
        val taskClass = loadClass(taskName)
        @Suppress("unchecked_cast")
        val constructor = taskClass.getDeclaredConstructor(loadClass("sandbox.java.util.function.Function"))
                as Constructor<out Function<in Any?, out Any?>>
        return BiFunction { userTask, input ->
            try {
                constructor.newInstance(userTask).apply(input)
            } catch (ex: Throwable) {
                val target = (ex as? InvocationTargetException)?.targetException ?: ex
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
    fun <T> createForImport(task: Function<in T?, out Any?>): Function<in T?, out Any?> {
        val taskClass = loadClass("sandbox.ImportTask")
        @Suppress("unchecked_cast")
        return taskClass.getDeclaredConstructor(Function::class.java).newInstance(task) as Function<in T?, out Any?>
    }

    /**
     * Given a class name, provide its corresponding [LoadedClass] for the sandbox.
     * This class may have been loaded by a parent classloader really.
     */
    @Throws(ClassNotFoundException::class)
    fun loadForSandbox(className: String): LoadedClass {
        val sandboxClass = loadClassForSandbox(className)
        val sandboxName = Type.getInternalName(sandboxClass)
        var loader = this
        while(true) {
            val loaded = loader.loadedClasses[sandboxName]
            if (loaded != null) {
                return loaded
            }
            loader = loader.parent as? SandboxClassLoader ?: return LoadedClass(sandboxClass, UNMODIFIED)
        }
    }

    @Throws(ClassNotFoundException::class)
    fun loadForSandbox(source: ClassSource): LoadedClass {
        return loadForSandbox(source.qualifiedClassName)
    }

    private fun loadClassForSandbox(className: String): Class<*> {
        val sandboxName = analysisConfiguration.classResolver.resolveNormalized(className)
        return try {
            loadClass(sandboxName)
        } finally {
            context.messages.acceptProvisional()
        }
    }

    @Throws(ClassNotFoundException::class)
    fun loadClassForSandbox(source: ClassSource): Class<*> {
        return loadClassForSandbox(source.qualifiedClassName)
    }

    @Throws(ClassNotFoundException::class)
    fun toSandboxClass(clazz: Class<*>): Class<*> {
        return loadClassForSandbox(ClassSource.fromClassName(clazz.name))
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
        synchronized (getClassLoadingLock(name)) {
            var clazz = findLoadedClass(name)
            if (clazz == null) {
                val source = ClassSource.fromClassName(name)
                val isSandboxClass = analysisConfiguration.isSandboxClass(source.internalClassName)

                if (!isSandboxClass || parent is SandboxClassLoader) {
                    try {
                        clazz = super.loadClass(name, false)
                    } catch (e: ClassNotFoundException) {
                    } catch (e: SandboxClassLoadingException) {
                        e.messages.clearProvisional()
                    }
                }

                if (clazz == null) {
                    if (isSandboxClass) {
                        clazz = loadSandboxClass(source, context).type
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
     */
    private fun loadSandboxClass(source: ClassSource, context: AnalysisContext): LoadedClass {
        return if (isDJVMException(source.internalClassName)) {
            /**
             * We need to load a DJVMException's owner class before we can create
             * its wrapper exception. And loading the owner should then also create
             * the wrapper class automatically.
             */
            loadedClasses.getOrElse(source.internalClassName) {
                val exceptionOwner = ClassSource.fromClassName(getDJVMExceptionOwner(source.qualifiedClassName))
                if (!analysisConfiguration.isJvmException(exceptionOwner.internalClassName)) {
                    /**
                     * JVM Exceptions belong to the parent classloader, and so will never
                     * be found inside a child classloader. Which means we must not try to
                     * create a duplicate inside any child classloaders either. Hence we
                     * re-invoke [loadClass] which will delegate back to the parent.
                     */
                    loadClass(exceptionOwner.qualifiedClassName, false)
                }
                loadedClasses[source.internalClassName]
            } ?: throw ClassNotFoundException(source.qualifiedClassName)
        } else {
            loadClassAndBytes(source, context).also { clazz ->
                /**
                 * Check whether we've just loaded an unpinned sandboxed throwable class.
                 * If we have, we may also need to synthesise a throwable wrapper for it.
                 */
                if (throwableClass.isAssignableFrom(clazz.type) && !analysisConfiguration.isJvmException(source.internalClassName)) {
                    logger.debug("Generating synthetic throwable for ${source.qualifiedClassName}")
                    loadWrapperFor(clazz.type)
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
    private fun loadClassAndBytes(request: ClassSource, context: AnalysisContext): LoadedClass {
        logger.debug("Loading class {}, origin={}...", request.qualifiedClassName, request.origin)
        val requestedPath = request.internalClassName
        val sourceName = analysisConfiguration.classResolver.reverseNormalized(request.qualifiedClassName)
        val resolvedName = analysisConfiguration.classResolver.resolveNormalized(sourceName)

        // Check if the class has already been loaded.
        val loadedClass = loadedClasses[requestedPath]
        if (loadedClass != null) {
            logger.trace("Class {} already loaded", request.qualifiedClassName)
            return loadedClass
        } else if (analysisConfiguration.isPinnedClass(requestedPath)) {
            logger.error("Class {} should not be loaded here", request.qualifiedClassName)
            throw SandboxClassLoadingException("Refusing to load pinned ${request.qualifiedClassName}", context)
        }

        val byteCode = if (analysisConfiguration.isTemplateClass(requestedPath)) {
            loadUnmodifiedByteCode(requestedPath)
        } else {
            // Load the byte code for the specified class.
            val reader = supportingClassLoader.classReader(sourceName, context, request.origin)

            // Analyse the class if not matching the whitelist.
            val readClassName = reader.className
            if (!analysisConfiguration.whitelist.matches(readClassName)) {
                logger.trace("Class {} does not match with the whitelist", request.qualifiedClassName)
                logger.trace("Analyzing class {}...", request.qualifiedClassName)
                analyzer.analyze(reader, context, SKIP_FRAMES)
            }

            // Check if any errors were found during analysis.
            context.messages.acceptProvisional()
            if (context.messages.errorCount > 0) {
                logger.debug("Errors detected after analyzing class {}", request.qualifiedClassName)
                throw SandboxClassLoadingException("Analysis failed for ${request.qualifiedClassName}", context)
            }

            // Transform the class definition and byte code in accordance with provided rules.
            rewriter.rewrite(reader, context)
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

        // Cache transformed class.
        val classWithByteCode = LoadedClass(clazz, byteCode)
        loadedClasses[requestedPath] = classWithByteCode
        if (request.origin != null) {
            context.recordClassOrigin(sourceName, ClassReference(request.origin))
        }

        logger.debug("Loaded class {}, bytes={}, isModified={}",
                request.qualifiedClassName, byteCode.bytes.size, byteCode.isModified)

        return classWithByteCode
    }

    private fun defineClass(name: String, byteCode: ByteCode): Class<*> {
        val idx = name.lastIndexOf('.')
        if (idx > 0) {
            val packageName = name.substring(0, idx)
            if (getPackage(packageName) == null) {
                definePackage(packageName, null, null, null, null, null, null, null)
            }
        }
        return defineClass(name, byteCode.bytes, 0, byteCode.bytes.size)
    }

    private fun loadUnmodifiedByteCode(internalClassName: String): ByteCode {
        return ByteCode((getSystemResourceAsStream("$internalClassName.class")
                ?: throw ClassNotFoundException(internalClassName)).readBytes(), false)
    }

    /**
     * Check whether the synthetic throwable wrapper already
     * exists for this exception, and create it if it doesn't.
     */
    private fun loadWrapperFor(throwable: Class<*>): LoadedClass {
        val className = analysisConfiguration.exceptionResolver.getThrowableName(throwable)
        return loadedClasses.getOrPut(className) {
            val superName = analysisConfiguration.exceptionResolver.getThrowableSuperName(throwable)
            val byteCode = ThrowableWrapperFactory.toByteCode(className, superName)
            LoadedClass(defineClass(className.asPackagePath, byteCode), byteCode)
        }
    }

    /**
     * Allow access to resources in the source classloader.
     */
    override fun getResource(resourceName: String): URL? {
        return supportingClassLoader.getResource(resourceName)
    }

    companion object {
        private val logger = loggerFor<SandboxClassLoader>()
        private val UNMODIFIED = ByteCode(ByteArray(0), false)

        /**
         * Factory function to create a [SandboxClassLoader].
         * @param configuration The [SandboxConfiguration] containing the classloader's configuration parameters.
         */
        fun createFor(configuration: SandboxConfiguration): SandboxClassLoader {
            val analysisConfiguration = configuration.analysisConfiguration
            val supportingClassLoader = analysisConfiguration.supportingClassLoader
            val parentClassLoader = configuration.parentClassLoader

            return SandboxClassLoader(
                analysisConfiguration = analysisConfiguration,
                supportingClassLoader = supportingClassLoader,
                ruleValidator = RuleValidator(rules = configuration.rules,
                                              configuration = analysisConfiguration),
                rewriter = ClassRewriter(configuration, supportingClassLoader),
                context = AnalysisContext.fromConfiguration(analysisConfiguration),
                throwableClass = parentClassLoader?.throwableClass,
                parent = parentClassLoader
            )
        }
    }

}
