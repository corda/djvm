package net.corda.djvm.serialization

import net.corda.core.serialization.ConstructorForDeserialization
import net.corda.core.serialization.CordaSerializable
import net.corda.core.serialization.DeprecatedConstructorForDeserialization
import net.corda.djvm.SandboxConfiguration
import net.corda.djvm.SandboxRuntimeContext
import net.corda.djvm.analysis.AnalysisConfiguration
import net.corda.djvm.analysis.Whitelist.Companion.MINIMAL
import net.corda.djvm.execution.ExecutionProfile.*
import net.corda.djvm.messages.Severity
import net.corda.djvm.messages.Severity.*
import net.corda.djvm.source.BootstrapClassLoader
import net.corda.djvm.source.UserPathSource
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.fail
import java.io.File
import java.nio.file.Files.exists
import java.nio.file.Files.isDirectory
import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.Consumer
import kotlin.concurrent.thread

@Suppress("unused", "MemberVisibilityCanBePrivate")
abstract class TestBase(type: SandboxType) {
    companion object {
        const val SANDBOX_STRING = "sandbox.java.lang.String"

        @JvmField
        val DETERMINISTIC_RT: Path = Paths.get(
            System.getProperty("deterministic-rt.path") ?: fail("deterministic-rt.path property not set"))

        @JvmField
        val TESTING_LIBRARIES: List<Path> = (System.getProperty("sandbox-libraries.path") ?: fail("sandbox-libraries.path property not set"))
                .split(File.pathSeparator).map { Paths.get(it) }.filter { exists(it) }

        private lateinit var bootstrapClassLoader: BootstrapClassLoader
        private lateinit var parentConfiguration: SandboxConfiguration

        @BeforeAll
        @JvmStatic
        fun setupClassLoader() {
            bootstrapClassLoader = BootstrapClassLoader(DETERMINISTIC_RT)
            val rootConfiguration = AnalysisConfiguration.createRoot(
                userSource = UserPathSource(emptyList()),
                whitelist = MINIMAL,
                visibleAnnotations = setOf(
                    CordaSerializable::class.java,
                    ConstructorForDeserialization::class.java,
                    DeprecatedConstructorForDeserialization::class.java
                ),
                bootstrapSource = bootstrapClassLoader
            )
            parentConfiguration = SandboxConfiguration.createFor(
                analysisConfiguration = rootConfiguration,
                profile = UNLIMITED,
                enableTracing = false
            )
        }

        @AfterAll
        @JvmStatic
        fun destroyRootContext() {
            bootstrapClassLoader.close()
        }
    }

    val classPaths: List<Path> = when(type) {
        SandboxType.KOTLIN -> TESTING_LIBRARIES
        SandboxType.JAVA -> TESTING_LIBRARIES.filter { isDirectory(it) }
    }

    fun sandbox(action: SandboxRuntimeContext.() -> Unit) {
        return sandbox(WARNING, emptySet(), emptySet(), action)
    }

    fun sandbox(visibleAnnotations: Set<Class<out Annotation>>, action: SandboxRuntimeContext.() -> Unit) {
        return sandbox(WARNING, visibleAnnotations, emptySet(), action)
    }

    fun sandbox(visibleAnnotations: Set<Class<out Annotation>>, sandboxOnlyAnnotations: Set<String>, action: SandboxRuntimeContext.() -> Unit) {
        return sandbox(WARNING, visibleAnnotations, sandboxOnlyAnnotations, action)
    }

    fun sandbox(
        minimumSeverityLevel: Severity,
        visibleAnnotations: Set<Class<out Annotation>>,
        sandboxOnlyAnnotations: Set<String>,
        action: SandboxRuntimeContext.() -> Unit
    ) {
        var thrownException: Throwable? = null
        thread {
            try {
                UserPathSource(classPaths).use { userSource ->
                    SandboxRuntimeContext(parentConfiguration.createChild(userSource, Consumer {
                        it.withNewMinimumSeverityLevel(minimumSeverityLevel)
                            .withSandboxOnlyAnnotations(sandboxOnlyAnnotations)
                            .withVisibleAnnotations(visibleAnnotations)
                    })).use {
                        action(this)
                    }
                }
            } catch (exception: Throwable) {
                thrownException = exception
            }
        }.join()
        throw thrownException ?: return
    }
}
