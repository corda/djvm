package net.corda.djvm.execution

import net.corda.djvm.SandboxType.KOTLIN
import net.corda.djvm.TestBase
import net.corda.djvm.code.asResourcePath
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import java.net.URL
import java.util.function.Function

class LoadingUserResourcesTest : TestBase(KOTLIN) {
    companion object {
        const val relativeResourceName = "local-resource.txt"
        val absoluteResourceName = (this::class.java.`package`.name).asResourcePath + '/' + relativeResourceName
    }

    @Test
    fun `test can load user resources from system classloader`() {
        val resources = GetSystemClassLoaderResources().apply(absoluteResourceName)
        assertThat(resources).isNotEmpty

        parentedSandbox {
            val executor = DeterministicSandboxExecutor<String, Array<String>>(configuration)
            executor.run<GetSystemClassLoaderResources>(absoluteResourceName).apply {
                assertThat(result).isEqualTo(resources)
            }
        }
    }

    class GetSystemClassLoaderResources : Function<String, Array<String>> {
        override fun apply(resourceName: String): Array<String> {
            return ClassLoader.getSystemClassLoader().getResources(resourceName).asSequence()
                    .mapTo(ArrayList(), URL::toString).toTypedArray()
        }
    }

    @Test
    fun `test can load user resources from classloader`() {
        val resources = GetClassLoaderResources().apply(absoluteResourceName)
        assertThat(resources).isNotEmpty

        parentedSandbox {
            val executor = DeterministicSandboxExecutor<String, Array<String>>(configuration)
            executor.run<GetClassLoaderResources>(absoluteResourceName).apply {
                assertThat(result).isEqualTo(resources)
            }
        }
    }

    class GetClassLoaderResources : Function<String, Array<String>> {
        override fun apply(resourceName: String): Array<String> {
            return javaClass.classLoader.getResources(resourceName).asSequence()
                    .mapTo(ArrayList(), URL::toString).toTypedArray()
        }
    }

    /**
     * Loading a user resource as a URL....
     */
    @Test
    fun `test can load user resource URL from system classloader`() {
        val targetPath = GetSystemClassLoaderResourceURL().apply(absoluteResourceName)
        assertThat(targetPath).isNotNull()

        parentedSandbox {
            val executor = DeterministicSandboxExecutor<String, String?>(configuration)
            executor.run<GetSystemClassLoaderResourceURL>(absoluteResourceName).apply {
                assertThat(result).isEqualTo(targetPath)
            }
        }
    }

    class GetSystemClassLoaderResourceURL : Function<String, String?> {
        override fun apply(resourceName: String): String? {
            return ClassLoader.getSystemClassLoader().getResource(resourceName)?.path
        }
    }

    @Test
    fun `test can load user resource URL from classloader`() {
        val targetPath = GetClassLoaderResourceURL().apply(absoluteResourceName)
        assertThat(targetPath).isNotNull()

        parentedSandbox {
            val executor = DeterministicSandboxExecutor<String, String?>(configuration)
            executor.run<GetClassLoaderResourceURL>(absoluteResourceName).apply {
                assertThat(result).isEqualTo(targetPath)
            }
        }
    }

    class GetClassLoaderResourceURL : Function<String, String?> {
        override fun apply(resourceName: String): String? {
            return javaClass.classLoader.getResource(resourceName)?.path
        }
    }

    @Test
    fun `test load user resource URL from class`() {
        val targetPath = GetClassResourceURL().apply(relativeResourceName)
        assertThat(targetPath).isNotNull()

        parentedSandbox {
            val executor = DeterministicSandboxExecutor<String, String?>(configuration)
            executor.run<GetClassResourceURL>(relativeResourceName).apply {
                assertThat(result).isEqualTo(targetPath)
            }
        }
    }

    class GetClassResourceURL : Function<String, String?> {
        override fun apply(resourceName: String): String? {
            return javaClass.getResource(resourceName)?.path
        }
    }

    /**
     * Loading a user resource as a stream....
     */
    @Test
    fun `test load user resource as stream from class`() {
        val contents = GetClassResourceStream().apply(relativeResourceName)
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
    fun `test load user resource as stream from classloader`() {
        val contents = GetClassLoaderResourceStream().apply(absoluteResourceName)
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
    fun `test load user resource as stream from system classloader`() {
        val contents = GetSystemClassLoaderResourceStream().apply(absoluteResourceName)
        assertThat(contents).isNotEmpty()

        parentedSandbox {
            val executor = DeterministicSandboxExecutor<String, ByteArray?>(configuration)
            executor.run<GetSystemClassLoaderResourceStream>(absoluteResourceName).apply {
                assertThat(result).isEqualTo(contents)
            }
        }
    }

    class GetSystemClassLoaderResourceStream : Function<String, ByteArray?> {
        override fun apply(resourceName: String): ByteArray? {
            return ClassLoader.getSystemClassLoader().getResourceAsStream(resourceName)?.readBytes()
        }
    }
}
