package net.corda.djvm.execution

import net.corda.djvm.SandboxType.KOTLIN
import net.corda.djvm.TestBase
import net.corda.djvm.code.asResourcePath
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.function.Function

class LoadingResourcesTest : TestBase(KOTLIN) {
    companion object {
        const val manifestResourceName = "META-INF/MANIFEST.MF"

        const val relativeResourceName = "local-resource.txt"
        val absoluteResourceName = (this::class.java.`package`.name).asResourcePath + '/' + relativeResourceName
    }

    @Test
    fun `test users cannot load system resources`() = parentedSandbox {
        val executor = DeterministicSandboxExecutor<String, Boolean>(configuration)
        executor.run<GetSystemResources>(manifestResourceName).apply {
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
        executor.run<GetSystemResourceURL>(manifestResourceName).apply {
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
        val executor = DeterministicSandboxExecutor<String, ByteArray?>(configuration)
        executor.run<GetSystemResourceStream>(manifestResourceName).apply {
            assertThat(result).isNull()
        }
    }

    class GetSystemResourceStream : Function<String, ByteArray?> {
        override fun apply(resourceName: String): ByteArray? {
            return ClassLoader.getSystemResourceAsStream(resourceName)?.readBytes()
        }
    }

    @Test
    fun `test users cannot load resources`() = parentedSandbox {
        val executor = DeterministicSandboxExecutor<String, Boolean>(configuration)
        executor.run<GetResources>(manifestResourceName).apply {
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
        executor.run<GetResourceURL>(manifestResourceName).apply {
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
        assertThat(javaClass.getResource(relativeResourceName)).isNotNull()

        parentedSandbox {
            val executor = DeterministicSandboxExecutor<String, String?>(configuration)
            executor.run<GetClassResourceURL>(relativeResourceName).apply {
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
    fun `test users can load class resource stream`() {
        val contents = javaClass.getResourceAsStream(relativeResourceName)?.readBytes()
        assertThat(contents).isNotEmpty()

        parentedSandbox {
            val executor = DeterministicSandboxExecutor<String, ByteArray?>(configuration)
            executor.run<GetClassResourceStream>(relativeResourceName).apply {
                assertThat(result).isEqualTo(contents)
            }
        }
    }

    class GetClassResourceStream : Function<String, ByteArray?> {
        override fun apply(resourceName: String): ByteArray? {
            return javaClass.getResourceAsStream(resourceName)?.readBytes()
        }
    }

    @Test
    fun `test users can load classloader resource stream`() {
        val contents = javaClass.classLoader.getResourceAsStream(absoluteResourceName)?.readBytes()
        assertThat(contents).isNotEmpty()

        parentedSandbox {
            val executor = DeterministicSandboxExecutor<String, ByteArray?>(configuration)
            executor.run<GetClassLoaderResourceStream>(absoluteResourceName).apply {
                assertThat(result).isEqualTo(contents)
            }
        }
    }

    class GetClassLoaderResourceStream : Function<String, ByteArray?> {
        override fun apply(resourceName: String): ByteArray? {
            return javaClass.classLoader.getResourceAsStream(resourceName)?.readBytes()
        }
    }

    @Test
    fun `test users can load system classloader resource stream`() {
        val contents = javaClass.classLoader.getResourceAsStream(absoluteResourceName)?.readBytes()
        assertThat(contents).isNotEmpty()

        parentedSandbox {
            val executor = DeterministicSandboxExecutor<String, ByteArray?>(configuration)
            executor.run<GetSystemClassLoaderResourceStream>(absoluteResourceName).apply {
                assertThat(result).isNotEmpty()
            }
        }
    }

    class GetSystemClassLoaderResourceStream : Function<String, ByteArray?> {
        override fun apply(resourceName: String): ByteArray? {
            return ClassLoader.getSystemClassLoader().getResourceAsStream(resourceName)?.readBytes()
        }
    }
}
