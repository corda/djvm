package net.corda.djvm.serialization

import net.corda.core.serialization.CordaSerializable
import net.corda.core.serialization.SerializationContext
import net.corda.core.serialization.SerializationContext.UseCase
import net.corda.core.serialization.internal.SerializationEnvironment
import net.corda.djvm.SandboxConfiguration
import net.corda.djvm.SandboxConfiguration.Companion.ALL_DEFINITION_PROVIDERS
import net.corda.djvm.SandboxConfiguration.Companion.ALL_EMITTERS
import net.corda.djvm.SandboxConfiguration.Companion.ALL_RULES
import net.corda.djvm.SandboxRuntimeContext
import net.corda.djvm.analysis.AnalysisConfiguration
import net.corda.djvm.analysis.Whitelist.Companion.MINIMAL
import net.corda.djvm.execution.ExecutionProfile.*
import net.corda.djvm.execution.SandboxRuntimeException
import net.corda.djvm.messages.Severity
import net.corda.djvm.messages.Severity.*
import net.corda.djvm.rewiring.SandboxClassLoader
import net.corda.djvm.source.BootstrapClassLoader
import net.corda.djvm.source.UserPathSource
import net.corda.serialization.internal.BuiltInExceptionsWhitelist
import net.corda.serialization.internal.GlobalTransientClassWhiteList
import net.corda.serialization.internal.SerializationContextImpl
import net.corda.serialization.internal.SerializationFactoryImpl
import net.corda.serialization.internal.amqp.amqpMagic
import net.corda.serialization.internal.amqp.createSerializerFactoryFactory
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.fail
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.nio.file.Files.exists
import java.nio.file.Files.isDirectory
import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.BiFunction
import kotlin.concurrent.thread

@Suppress("unused", "MemberVisibilityCanBePrivate")
abstract class TestBase(type: SandboxType) {
    companion object {
        @JvmField
        val DETERMINISTIC_RT: Path = Paths.get(
            System.getProperty("deterministic-rt.path") ?: fail("deterministic-rt.path property not set"))

        @JvmField
        val TESTING_LIBRARIES: List<Path> = (System.getProperty("sandbox-libraries.path") ?: fail("sandbox-libraries.path property not set"))
                .split(File.pathSeparator).map { Paths.get(it) }.filter { exists(it) }

        private lateinit var configuration: SandboxConfiguration
        private lateinit var classLoader: SandboxClassLoader

        @BeforeAll
        @JvmStatic
        fun setupClassLoader() {
            val rootConfiguration = AnalysisConfiguration.createRoot(
                userSource = UserPathSource(emptyList()),
                whitelist = MINIMAL,
                visibleAnnotations = setOf(CordaSerializable::class.java),
                bootstrapSource = BootstrapClassLoader(DETERMINISTIC_RT)
            )
            configuration = SandboxConfiguration.of(
                UNLIMITED,
                ALL_RULES,
                ALL_EMITTERS,
                ALL_DEFINITION_PROVIDERS,
                true,
                rootConfiguration
            )
            classLoader = SandboxClassLoader.createFor(configuration)
        }

        @AfterAll
        @JvmStatic
        fun destroyRootContext() {
            configuration.analysisConfiguration.closeAll()
        }
    }

    val classPaths: List<Path> = when(type) {
        SandboxType.KOTLIN -> TESTING_LIBRARIES
        SandboxType.JAVA -> TESTING_LIBRARIES.filter { isDirectory(it) }
    }

    fun sandbox(action: SandboxRuntimeContext.() -> Unit) {
        return sandbox(WARNING, emptySet(), false, action)
    }

    fun sandbox(visibleAnnotations: Set<Class<out Annotation>>, action: SandboxRuntimeContext.() -> Unit) {
        return sandbox(WARNING, visibleAnnotations, false, action)
    }

    fun sandbox(
        minimumSeverityLevel: Severity,
        visibleAnnotations: Set<Class<out Annotation>>,
        enableTracing: Boolean,
        action: SandboxRuntimeContext.() -> Unit
    ) {
        var thrownException: Throwable? = null
        thread {
            try {
                configuration.analysisConfiguration.createChild(
                    userSource = UserPathSource(classPaths),
                    newMinimumSeverityLevel = minimumSeverityLevel,
                    visibleAnnotations = visibleAnnotations
                ).use { analysisConfiguration ->
                    SandboxRuntimeContext(SandboxConfiguration.of(
                        configuration.executionProfile,
                        configuration.rules,
                        configuration.emitters,
                        configuration.definitionProviders,
                        enableTracing,
                        analysisConfiguration,
                        classLoader
                    )).use {
                        action(this)
                    }
                }
            } catch (exception: Throwable) {
                thrownException = exception
            }
        }.join()
        throw thrownException ?: return
    }

    fun createSandboxSerializationEnv(classLoader: SandboxClassLoader): SerializationEnvironment {
        val p2pContext: SerializationContext = SerializationContextImpl(
            preferredSerializationVersion = amqpMagic,
            deserializationClassLoader = DelegatingClassLoader(classLoader),
            whitelist = GlobalTransientClassWhiteList(BuiltInExceptionsWhitelist()),
            properties = emptyMap(),
            objectReferencesEnabled = true,
            useCase = UseCase.P2P,
            encoding = null
        )

        val factory = SerializationFactoryImpl(mutableMapOf()).apply {
            registerScheme(SandboxAMQPSerializationScheme(classLoader, createSerializerFactoryFactory()))
        }
        return SerializationEnvironment.with(factory, p2pContext = p2pContext)
    }

    fun createExecutorFor(classLoader: SandboxClassLoader): BiFunction<in Any, in Any?, out Any?> {
        val taskClass = classLoader.loadClass("sandbox.RawTask")
        val taskApply = taskClass.getDeclaredMethod("apply", Any::class.java)
        val taskConstructor = taskClass.getDeclaredConstructor(classLoader.loadClass("sandbox.java.util.function.Function"))
        return BiFunction { userTask, arg ->
            try {
                taskApply(taskConstructor.newInstance(userTask), arg)
            } catch (ex: InvocationTargetException) {
                val target = ex.targetException
                throw when (target) {
                    is RuntimeException, is Error -> target
                    else -> SandboxRuntimeException(target.message, target)
                }
            }
        }
    }
}