package com.example.testing

import net.corda.core.serialization.CordaSerializable
import net.corda.djvm.SandboxConfiguration
import net.corda.djvm.SandboxRuntimeContext
import net.corda.djvm.analysis.AnalysisConfiguration
import net.corda.djvm.analysis.Whitelist
import net.corda.djvm.execution.ExecutionProfile
import net.corda.djvm.messages.Severity
import net.corda.djvm.rewiring.SandboxClassLoader
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
import kotlin.concurrent.thread

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
                whitelist = Whitelist.MINIMAL,
                visibleAnnotations = setOf(CordaSerializable::class.java),
                bootstrapSource = BootstrapClassLoader(DETERMINISTIC_RT)
            )
            configuration = SandboxConfiguration.of(
                ExecutionProfile.UNLIMITED,
                SandboxConfiguration.ALL_RULES,
                SandboxConfiguration.ALL_EMITTERS,
                SandboxConfiguration.ALL_DEFINITION_PROVIDERS,
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
        return sandbox(Severity.WARNING, emptySet(), false, action)
    }

    fun sandbox(visibleAnnotations: Set<Class<out Annotation>>, action: SandboxRuntimeContext.() -> Unit) {
        return sandbox(Severity.WARNING, visibleAnnotations, false, action)
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
}