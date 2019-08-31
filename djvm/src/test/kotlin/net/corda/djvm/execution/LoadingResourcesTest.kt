package net.corda.djvm.execution

import net.corda.djvm.SandboxType.KOTLIN
import net.corda.djvm.TestBase
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.function.Function

class LoadingResourcesTest : TestBase(KOTLIN) {
    @Test
    fun `test users cannot load system resources`() = parentedSandbox {
        val executor = DeterministicSandboxExecutor<String, Boolean>(configuration)
        executor.run<GetSystemResources>("META-INF/MANIFEST.MF").apply {
            assertThat(result).isFalse()
        }
    }

    class GetSystemResources : Function<String, Boolean> {
        override fun apply(resourceName: String): Boolean {
            return ClassLoader.getSystemResources(resourceName).hasMoreElements()
        }
    }

    @Test
    fun `test users cannot load system resource URL`() = parentedSandbox {
        val executor = DeterministicSandboxExecutor<String, String?>(configuration)
        executor.run<GetSystemResourceURL>("META-INF/MANIFEST.MF").apply {
            assertThat(result).isNull()
        }
    }

    class GetSystemResourceURL : Function<String, String?> {
        override fun apply(resourceName: String): String? {
            return ClassLoader.getSystemResource(resourceName)?.path
        }
    }

    @Test
    fun `test users cannot load system resource stream`() = parentedSandbox {
        val executor = DeterministicSandboxExecutor<String, Int?>(configuration)
        executor.run<GetSystemResourceStream>("META-INF/MANIFEST.MF").apply {
            assertThat(result).isNull()
        }
    }

    class GetSystemResourceStream : Function<String, Int?> {
        override fun apply(resourceName: String): Int? {
            return ClassLoader.getSystemResourceAsStream(resourceName)?.available()
        }
    }

    @Test
    fun `test users cannot load resources`() = parentedSandbox {
        val executor = DeterministicSandboxExecutor<String, Boolean>(configuration)
        executor.run<GetResources>("META-INF/MANIFEST.MF").apply {
            assertThat(result).isFalse()
        }
    }

    class GetResources : Function<String, Boolean> {
        override fun apply(resourceName: String): Boolean {
            return ClassLoader.getSystemClassLoader().getResources(resourceName).hasMoreElements()
        }
    }

    @Test
    fun `test users cannot load resource URL`() = parentedSandbox {
        val executor = DeterministicSandboxExecutor<String, String?>(configuration)
        executor.run<GetResourceURL>("META-INF/MANIFEST.MF").apply {
            assertThat(result).isNull()
        }
    }

    class GetResourceURL : Function<String, String?> {
        override fun apply(resourceName: String): String? {
            return ClassLoader.getSystemClassLoader().getResource(resourceName)?.path
        }
    }

    @Test
    fun `test users cannot load class resource URL`() {
        val resourceName = "local-resource.txt"
        assertThat(javaClass.getResource(resourceName)).isNotNull()

        parentedSandbox {
            val executor = DeterministicSandboxExecutor<String, String?>(configuration)
            executor.run<GetClassResourceURL>(resourceName).apply {
                assertThat(result).isNull()
            }
        }
    }

    class GetClassResourceURL : Function<String, String?> {
        override fun apply(resourceName: String): String? {
            return javaClass.getResource(resourceName)?.path
        }
    }

    @Test
    fun `test users cannot load resource stream`() = parentedSandbox {
        val executor = DeterministicSandboxExecutor<String, Int?>(configuration)
        executor.run<GetResourceStream>("META-INF/MANIFEST.MF").apply {
            assertThat(result).isNull()
        }
    }

    class GetResourceStream : Function<String, Int?> {
        override fun apply(resourceName: String): Int? {
            return ClassLoader.getSystemClassLoader().getResourceAsStream(resourceName)?.available()
        }
    }
}
