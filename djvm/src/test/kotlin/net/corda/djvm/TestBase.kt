package net.corda.djvm

import foo.bar.sandbox.Callable
import net.corda.djvm.SandboxConfiguration.Companion.ALL_DEFINITION_PROVIDERS
import net.corda.djvm.SandboxConfiguration.Companion.ALL_EMITTERS
import net.corda.djvm.SandboxConfiguration.Companion.ALL_RULES
import net.corda.djvm.SandboxType.JAVA
import net.corda.djvm.SandboxType.KOTLIN
import net.corda.djvm.analysis.AnalysisConfiguration
import net.corda.djvm.analysis.AnalysisContext
import net.corda.djvm.analysis.ClassAndMemberVisitor
import net.corda.djvm.analysis.Whitelist
import net.corda.djvm.assertions.AssertionExtensions.assertThat
import net.corda.djvm.code.DefinitionProvider
import net.corda.djvm.code.Emitter
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
import net.corda.djvm.validation.RuleValidator
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.fail
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Type
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.nio.file.Files.exists
import java.nio.file.Files.isDirectory
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Collections.unmodifiableList
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import kotlin.concurrent.thread
import kotlin.reflect.jvm.jvmName

@Suppress("unused")
@ExtendWith(SecurityManagement::class)
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

        @JvmField
        val TEST_WHITELIST = Whitelist.MINIMAL + setOf("^net/corda/djvm/Utilities(\\..*)?\$".toRegex())

        private lateinit var parentConfiguration: SandboxConfiguration
        private lateinit var bootstrapClassLoader: BootstrapClassLoader

        /**
         * Get the full name of type [T].
         */
        inline fun <reified T> nameOf(prefix: String = "") = "$prefix${Type.getInternalName(T::class.java)}"

        @BeforeAll
        @JvmStatic
        fun setupRootClassLoader() {
            bootstrapClassLoader = BootstrapClassLoader(DETERMINISTIC_RT)
            val rootConfiguration = AnalysisConfiguration.createRoot(
                userSource = UserPathSource(emptyList()),
                whitelist = TEST_WHITELIST,
                bootstrapSource = bootstrapClassLoader
            )
            parentConfiguration = SandboxConfiguration.createFor(
                analysisConfiguration = rootConfiguration,
                profile = ExecutionProfile.UNLIMITED
            )
        }

        @AfterAll
        @JvmStatic
        fun destroyRootContext() {
            bootstrapClassLoader.close()
        }
    }

    val classPaths: List<Path> = when(type) {
        KOTLIN -> TESTING_LIBRARIES
        JAVA -> TESTING_LIBRARIES.filter { isDirectory(it) }
    }

    private val userSource = UserPathSource(classPaths)

    /**
     * Default analysis configuration.
     */
    val configuration = AnalysisConfiguration.createRoot(
        userSource = userSource,
        whitelist = TEST_WHITELIST,
        bootstrapSource = bootstrapClassLoader
    )

    /**
     * Default analysis context
     */
    val context: AnalysisContext
        get() = AnalysisContext.fromConfiguration(configuration)

    @AfterEach
    fun destroy() {
        userSource.close()
    }

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
                whitelist = TEST_WHITELIST,
                minimumSeverityLevel = minimumSeverityLevel,
                bootstrapSource = bootstrapClassLoader
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
        var whitelist = TEST_WHITELIST
        for (option in options) {
            when (option) {
                is Rule -> rules.add(option)
                is Emitter -> emitters.add(option)
                is DefinitionProvider -> definitionProviders.add(option)
                is ExecutionProfile -> profile = option
                is ClassSource -> classSources.add(option)
                is Whitelist -> whitelist = option
                is List<*> -> {
                    rules.addAll(option.filterIsInstance<Rule>())
                    emitters.addAll(option.filterIsInstance<Emitter>())
                    definitionProviders.addAll(option.filterIsInstance<DefinitionProvider>())
                }
            }
        }
        var thrownException: Throwable? = null
        val executionProfile = if (enableTracing) { profile } else { null }
        thread(start = false, name = "DJVM-${javaClass.name}-${threadId.getAndIncrement()}") {
            UserPathSource(classPaths).use { userSource ->
                val analysisConfiguration = AnalysisConfiguration.createRoot(
                    userSource = userSource,
                    whitelist = whitelist,
                    visibleAnnotations = visibleAnnotations,
                    minimumSeverityLevel = minimumSeverityLevel,
                    bootstrapSource = bootstrapClassLoader
                )
                SandboxRuntimeContext(SandboxConfiguration.of(
                    executionProfile,
                    rules.distinctBy(Any::javaClass),
                    emitters.distinctBy(Any::javaClass),
                    definitionProviders.distinctBy(Any::javaClass),
                    analysisConfiguration,
                    externalCache
                )).use(Consumer { ctx ->
                    assertThat(ctx.runtimeCosts).areZero()
                    ctx.action()
                })
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

    inline fun sandbox(visibleAnnotations: Set<Class<out Annotation>>, crossinline action: SandboxRuntimeContext.() -> Unit) {
        sandbox(visibleAnnotations, Consumer { ctx -> action(ctx) })
    }

    fun sandbox(visibleAnnotations: Set<Class<out Annotation>>, action: Consumer<SandboxRuntimeContext>) {
        sandbox(WARNING, visibleAnnotations, null, action)
    }

    inline fun sandbox(externalCache: ExternalCache, crossinline action: SandboxRuntimeContext.() -> Unit) {
        sandbox(externalCache, Consumer { ctx -> action(ctx) })
    }

    fun sandbox(externalCache: ExternalCache, action: Consumer<SandboxRuntimeContext>) {
        sandbox(WARNING, emptySet(), externalCache, action)
    }

    inline fun sandbox(crossinline action: SandboxRuntimeContext.() -> Unit) {
        sandbox(Consumer { ctx -> action(ctx) })
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
        var thrownException: Throwable? = null
        val testAction = Consumer<SandboxRuntimeContext> { ctx ->
            assertThat(ctx.runtimeCosts).areZero()
        }.andThen(action)
        thread(start = false, name = "DJVM-${javaClass.name}-${threadId.getAndIncrement()}") {
            UserPathSource(classPaths).use { userSource ->
                SandboxRuntimeContext(parentConfiguration.createChild(userSource, Consumer {
                    it.setMinimumSeverityLevel(minimumSeverityLevel)
                    it.setVisibleAnnotations(visibleAnnotations)
                    it.setExternalCache(externalCache)
                })).use(testAction)
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

    /**
     * Get a class reference from a class hierarchy based on [T].
     */
    inline fun <reified T> ClassHierarchy.get() = this[nameOf<T>()]!!

    /**
     * Create a new instance of a class using the sandbox class loader.
     */
    inline fun <reified T : Callable> SandboxRuntimeContext.newCallable(): LoadedClass = loadClass<T>()

    inline fun <reified T : Any> SandboxRuntimeContext.loadClass(): LoadedClass = loadClass(T::class.jvmName)

    fun SandboxRuntimeContext.loadClass(className: String): LoadedClass = classLoader.loadForSandbox(className)

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
    protected class Writer : ClassWriter(COMPUTE_FRAMES) {
        init {
            assertEquals(ClassAndMemberVisitor.API_VERSION, api, "Incorrect ASM API version")
        }
    }

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
