package net.corda.djvm

import foo.bar.sandbox.Callable
import net.corda.djvm.SandboxConfiguration.Companion.ALL_DEFINITION_PROVIDERS
import net.corda.djvm.SandboxConfiguration.Companion.ALL_EMITTERS
import net.corda.djvm.SandboxConfiguration.Companion.ALL_RULES
import net.corda.djvm.SandboxType.JAVA
import net.corda.djvm.SandboxType.KOTLIN
import net.corda.djvm.analysis.AnalysisConfiguration
import net.corda.djvm.analysis.AnalysisContext
import net.corda.djvm.assertions.AssertionExtensions.assertThat
import net.corda.djvm.code.DefinitionProvider
import net.corda.djvm.code.Emitter
import net.corda.djvm.code.impl.SandboxRemapper
import net.corda.djvm.execution.ExecutionProfile
import net.corda.djvm.messages.Severity
import net.corda.djvm.messages.Severity.INFORMATIONAL
import net.corda.djvm.messages.Severity.WARNING
import net.corda.djvm.references.ClassHierarchy
import net.corda.djvm.rewiring.ExternalCache
import net.corda.djvm.rewiring.LoadedClass
import net.corda.djvm.rewiring.flushAll
import net.corda.djvm.rules.Rule
import net.corda.djvm.source.BootstrapClassLoader
import net.corda.djvm.source.ClassSource
import net.corda.djvm.source.UserPathSource
import net.corda.djvm.validation.impl.RuleValidator
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.fail
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Type
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.nio.file.Files.exists
import java.nio.file.Files.isRegularFile
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Collections.unmodifiableList
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import kotlin.concurrent.thread
import kotlin.reflect.jvm.jvmName

@Suppress("MemberVisibilityCanBePrivate")
@ExtendWith(SecurityManagement::class)
@TestInstance(PER_CLASS)
abstract class TestBase(type: SandboxType) {
    companion object {
        private val threadId = AtomicInteger(0)

        @JvmField
        val BLANK = emptySet<Any>()

        @JvmField
        val DEFAULT: List<Any> = unmodifiableList((ALL_RULES + ALL_EMITTERS + ALL_DEFINITION_PROVIDERS).distinctBy(Any::javaClass))

        @JvmField
        val DETERMINISTIC_RT: Path = Paths.get(
                System.getProperty("deterministic-rt.path") ?: fail("deterministic-rt.path property not set"))

        @JvmField
        val TESTING_LIBRARIES: List<Path> = (System.getProperty("sandbox-libraries.path") ?: fail("sandbox-libraries.path property not set"))
                .split(File.pathSeparator).map { Paths.get(it) }.filter { exists(it) }

        /**
         * This class does not belong to the sandbox package space
         * and so will be used directly without being duplicated.
         */
        @JvmField
        val TEST_OVERRIDES = setOf(Utilities::class.java.name)

        /**
         * Get the full name of type [T].
         */
        inline fun <reified T> nameOf(prefix: String = "") = "$prefix${Type.getInternalName(T::class.java)}"
    }

    private lateinit var parentConfiguration: SandboxConfiguration
    private lateinit var bootstrapClassLoader: BootstrapClassLoader

    @BeforeAll
    fun setupRootClassLoader() {
        bootstrapClassLoader = BootstrapClassLoader(DETERMINISTIC_RT)
        val rootConfiguration = AnalysisConfiguration.createRoot(
            userSource = UserPathSource(emptyList()),
            bootstrapSource = bootstrapClassLoader,
            overrideClasses = TEST_OVERRIDES
        )
        parentConfiguration = SandboxConfiguration.createFor(
            analysisConfiguration = rootConfiguration,
            profile = ExecutionProfile.UNLIMITED
        )
    }

    @AfterAll
    fun destroyRootContext() {
        bootstrapClassLoader.close()
        userSource.close()
    }

    val classPaths: List<Path> = when(type) {
        KOTLIN -> TESTING_LIBRARIES
        JAVA -> TESTING_LIBRARIES.filterNot(::isKotlin)
    }

    private fun isKotlin(path: Path): Boolean {
        return isRegularFile(path) && path.fileName.toString().contains("kotlin")
    }

    private val userSource = UserPathSource(classPaths)

    /**
     * Default analysis configuration.
     */
    val configuration by lazy {
        AnalysisConfiguration.createRoot(
            userSource = userSource,
            bootstrapSource = bootstrapClassLoader,
            overrideClasses = TEST_OVERRIDES
        )
    }

    val remapper: SandboxRemapper
        get() = with(configuration) { SandboxRemapper(classResolver, whitelist) }

    /**
     * Default analysis context
     */
    val context: AnalysisContext
        get() = AnalysisContext.fromConfiguration(configuration)

    fun flushInternalCache() {
        parentConfiguration.byteCodeCache.flushAll()
    }

    /**
     * Short-hand for analysing and validating a class.
     */
    inline fun <reified T> validate(
        minimumSeverityLevel: Severity = INFORMATIONAL,
        noinline block: (RuleValidator.(AnalysisContext) -> Unit)
    ) {
        return validate(ClassReader(T::class.java.name), minimumSeverityLevel, block)
    }

    fun validate(
        reader: ClassReader,
        minimumSeverityLevel: Severity,
        block: (RuleValidator.(AnalysisContext) -> Unit)
    ) {
        UserPathSource(classPaths).use { userSource ->
            val analysisConfiguration = AnalysisConfiguration.createRoot(
                userSource = userSource,
                minimumSeverityLevel = minimumSeverityLevel,
                bootstrapSource = bootstrapClassLoader,
                overrideClasses = TEST_OVERRIDES
            )
            val validator = RuleValidator(ALL_RULES, analysisConfiguration)
            val context = AnalysisContext.fromConfiguration(analysisConfiguration)
            validator.analyze(reader, context, 0)
            block(validator, context)
        }
    }

    /**
     * Run action on a separate thread to ensure that the code is run off a clean slate. The sandbox context is local to
     * the current thread, so this allows inspection of the cost summary object, etc. from within the provided delegate.
     */
    fun customSandbox(
        visibleAnnotations: Set<Class<out Annotation>>,
        action: SandboxRuntimeContext.() -> Unit
    ) {
        return customSandbox(
            visibleAnnotations = visibleAnnotations,
            minimumSeverityLevel = WARNING,
            enableTracing = true,
            action = action
        )
    }

    @Suppress("unused")
    fun customSandbox(action: SandboxRuntimeContext.() -> Unit) = customSandbox(emptySet(), action)

    fun customSandbox(
        vararg options: Any,
        visibleAnnotations: Set<Class<out Annotation>> = emptySet(),
        minimumSeverityLevel: Severity = WARNING,
        enableTracing: Boolean = true,
        externalCache: ExternalCache? = null,
        action: SandboxRuntimeContext.() -> Unit
    ) {
        val rules = mutableListOf<Rule>()
        val emitters = mutableListOf<Emitter>().apply { addAll(ALL_EMITTERS) }
        val definitionProviders = mutableListOf<DefinitionProvider>().apply { addAll(ALL_DEFINITION_PROVIDERS) }
        val classSources = mutableListOf<ClassSource>()
        var profile = ExecutionProfile.UNLIMITED
        for (option in options) {
            when (option) {
                is Rule -> rules.add(option)
                is Emitter -> emitters.add(option)
                is DefinitionProvider -> definitionProviders.add(option)
                is ExecutionProfile -> profile = option
                is ClassSource -> classSources.add(option)
                is List<*> -> {
                    rules.addAll(option.filterIsInstance<Rule>())
                    emitters.addAll(option.filterIsInstance<Emitter>())
                    definitionProviders.addAll(option.filterIsInstance<DefinitionProvider>())
                }
            }
        }
        var thrownException: Throwable? = null
        val testAction = Consumer<SandboxRuntimeContext> { ctx ->
            assertThat(ctx.runtimeCosts).areZero()
        }.andThen(action)
        val executionProfile = if (enableTracing) { profile } else { null }
        thread(start = false, name = "DJVM-${javaClass.name}-${threadId.getAndIncrement()}") {
            UserPathSource(classPaths).use { userSource ->
                val analysisConfiguration = AnalysisConfiguration.createRoot(
                    userSource = userSource,
                    visibleAnnotations = visibleAnnotations,
                    minimumSeverityLevel = minimumSeverityLevel,
                    bootstrapSource = bootstrapClassLoader,
                    overrideClasses = TEST_OVERRIDES
                )
                SandboxRuntimeContext(SandboxConfiguration.of(
                    executionProfile,
                    rules.distinctBy(Any::javaClass),
                    emitters.distinctBy(Any::javaClass),
                    definitionProviders.distinctBy(Any::javaClass),
                    analysisConfiguration,
                    externalCache
                )).use(testAction)
            }
        }.apply {
            uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, ex ->
                thrownException = ex
            }
            start()
            join()
        }
        throw thrownException ?: return
    }

    fun sandbox(visibleAnnotations: Set<Class<out Annotation>>, action: SandboxRuntimeContext.() -> Unit) {
        sandbox(visibleAnnotations, Consumer(action))
    }

    fun sandbox(visibleAnnotations: Set<Class<out Annotation>>, action: Consumer<SandboxRuntimeContext>) {
        sandbox(WARNING, visibleAnnotations, null, action)
    }

    fun sandbox(externalCache: ExternalCache, action: SandboxRuntimeContext.() -> Unit) {
        sandbox(externalCache, Consumer(action))
    }

    fun sandbox(externalCache: ExternalCache, action: Consumer<SandboxRuntimeContext>) {
        sandbox(WARNING, emptySet(), externalCache, action)
    }

    fun sandbox(action: SandboxRuntimeContext.() -> Unit) {
        sandbox(Consumer(action))
    }

    fun sandbox(action: Consumer<SandboxRuntimeContext>) {
        sandbox(WARNING, emptySet(), null, action)
    }

    fun sandbox(
        minimumSeverityLevel: Severity,
        visibleAnnotations: Set<Class<out Annotation>>,
        externalCache: ExternalCache?,
        action: Consumer<SandboxRuntimeContext>
    ) {
        create(options = Consumer {
            it.setMinimumSeverityLevel(minimumSeverityLevel)
            it.setVisibleAnnotations(visibleAnnotations)
            it.setExternalCache(externalCache)
        }, action = Consumer { ctx ->
            sandbox(ctx, action)
        })
    }

    fun create(action: SandboxRuntimeContext.() -> Unit) {
        create(Consumer(action))
    }

    fun create(action: Consumer<SandboxRuntimeContext>) {
        create(Consumer {}, action)
    }

    fun create(options: Consumer<ChildOptions>, action: Consumer<SandboxRuntimeContext>) {
        UserPathSource(classPaths).use { userSource ->
            action.accept(SandboxRuntimeContext(parentConfiguration.createChild(userSource, options)))
        }
    }

    fun sandbox(context: SandboxRuntimeContext, action: Consumer<SandboxRuntimeContext>) {
        var thrownException: Throwable? = null
        thread(start = false, name = "DJVM-${javaClass.name}-${threadId.getAndIncrement()}") {
            context.use(action)
        }.apply {
            uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, ex ->
                thrownException = ex
            }
            start()
            join()
        }
        throw thrownException ?: return
    }

    /**
     * @return an [AssertResetContext] to test the current resettable phase.
     */
    fun assertResetContextFor(ctx: SandboxRuntimeContext) = AssertResetContext(ctx.currentResetView)

    /**
     * Get a class reference from a class hierarchy based on [T].
     */
    inline fun <reified T> ClassHierarchy.get() = this[nameOf<T>()]!!

    /**
     * Create a new instance of a class using the sandbox class loader.
     */
    @Throws(ClassNotFoundException::class)
    inline fun <reified T : Callable> SandboxRuntimeContext.newCallable(): LoadedClass = loadClass<T>()

    @Throws(ClassNotFoundException::class)
    inline fun <reified T : Any> SandboxRuntimeContext.loadClass(): LoadedClass = loadClass(T::class.jvmName)

    @Throws(ClassNotFoundException::class)
    fun SandboxRuntimeContext.loadClass(className: String): LoadedClass = classLoader.loadForSandbox(className)

    @Throws(ClassNotFoundException::class)
    fun SandboxRuntimeContext.toSandboxClass(className: String): Class<*> = classLoader.toSandboxClass(className)

    @Throws(ClassNotFoundException::class)
    fun <T : Any> SandboxRuntimeContext.toSandboxClass(clazz: Class<T>): Class<*> = classLoader.toSandboxClass(clazz)

    @Throws(ClassNotFoundException::class)
    inline fun <reified T : Any> SandboxRuntimeContext.toSandboxClass(): Class<*> = toSandboxClass(T::class.java)

    /**
     * Run the entry-point of the loaded [Callable] class.
     */
    fun LoadedClass.createAndInvoke(methodName: String = "call") {
        val instance = type.getDeclaredConstructor().newInstance()
        val method = instance.javaClass.getMethod(methodName)
        try {
            method.invoke(instance)
        } catch (ex: InvocationTargetException) {
            throw ex.targetException
        }
    }

    /**
     * Stub visitor.
     */
    protected class Writer : ClassWriter(COMPUTE_FRAMES)

    @Suppress("MemberVisibilityCanBePrivate")
    protected class DJVM(private val classLoader: ClassLoader) {
        private val djvm: Class<*> = classFor("sandbox.java.lang.DJVM")
        val objectClass: Class<*> by lazy { classFor("sandbox.java.lang.Object") }
        val stringClass: Class<*> by lazy { classFor("sandbox.java.lang.String") }
        val longClass: Class<*> by lazy { classFor("sandbox.java.lang.Long") }
        val integerClass: Class<*> by lazy { classFor("sandbox.java.lang.Integer") }
        val shortClass: Class<*> by lazy { classFor("sandbox.java.lang.Short") }
        val byteClass: Class<*> by lazy { classFor("sandbox.java.lang.Byte") }
        val characterClass: Class<*> by lazy { classFor("sandbox.java.lang.Character") }
        val booleanClass: Class<*> by lazy { classFor("sandbox.java.lang.Boolean") }
        val doubleClass: Class<*> by lazy { classFor("sandbox.java.lang.Double") }
        val floatClass: Class<*> by lazy { classFor("sandbox.java.lang.Float") }
        val throwableClass: Class<*> by lazy { classFor("sandbox.java.lang.Throwable") }
        val stackTraceElementClass: Class<*> by lazy { classFor("sandbox.java.lang.StackTraceElement") }

        fun classFor(className: String): Class<*> = Class.forName(className, false, classLoader)

        fun sandbox(obj: Any): Any {
            return djvm.getMethod("sandbox", Any::class.java).invoke(null, obj)
        }

        fun unsandbox(obj: Any): Any {
            return djvm.getMethod("unsandbox", Any::class.java).invoke(null, obj)
        }

        fun stringOf(str: String): Any {
            return stringClass.getMethod("toDJVM", String::class.java).invoke(null, str)
        }

        fun longOf(l: Long): Any {
            return longClass.getMethod("toDJVM", Long::class.javaObjectType).invoke(null, l)
        }

        fun intOf(i: Int): Any {
            return integerClass.getMethod("toDJVM", Int::class.javaObjectType).invoke(null, i)
        }

        fun shortOf(i: Int): Any {
            return shortClass.getMethod("toDJVM", Short::class.javaObjectType).invoke(null, i.toShort())
        }

        fun byteOf(i: Int): Any {
            return byteClass.getMethod("toDJVM", Byte::class.javaObjectType).invoke(null, i.toByte())
        }

        fun charOf(c: Char): Any {
            return characterClass.getMethod("toDJVM", Char::class.javaObjectType).invoke(null, c)
        }

        fun booleanOf(bool: Boolean): Any {
            return booleanClass.getMethod("toDJVM", Boolean::class.javaObjectType).invoke(null, bool)
        }

        fun doubleOf(d: Double): Any {
            return doubleClass.getMethod("toDJVM", Double::class.javaObjectType).invoke(null, d)
        }

        fun floatOf(f: Float): Any {
            return floatClass.getMethod("toDJVM", Float::class.javaObjectType).invoke(null, f)
        }

        fun objectArrayOf(vararg objs: Any): Array<in Any> {
            @Suppress("unchecked_cast")
            return (java.lang.reflect.Array.newInstance(objectClass, objs.size) as Array<in Any>).also {
                for (i in objs.indices) {
                    it[i] = objectClass.cast(objs[i])
                }
            }
        }
    }

    fun Any.getArray(methodName: String): Array<*> = javaClass.getMethod(methodName).invoke(this) as Array<*>
}
