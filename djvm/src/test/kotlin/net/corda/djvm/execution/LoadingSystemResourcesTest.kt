package net.corda.djvm.execution

import net.corda.djvm.SandboxType.KOTLIN
import net.corda.djvm.TestBase
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import java.net.URL
import java.util.function.Function

class LoadingSystemResourcesTest : TestBase(KOTLIN) {
    companion object {
        const val manifestResourceName = "META-INF/MANIFEST.MF"
    }

    @Test
    fun `test users can load system resources`() = parentedSandbox {
        val executor = DeterministicSandboxExecutor<String, Array<String>>(configuration)
        executor.run<GetSystemResources>(manifestResourceName).apply {
            assertThat(result).isNotEmpty
        }
    }

    class GetSystemResources : Function<String, Array<String>> {
        override fun apply(resourceName: String): Array<String> {
            return ClassLoader.getSystemResources(resourceName)
                    .asSequence().mapTo(ArrayList(), URL::toString).toTypedArray()
        }
    }

    @Test
    fun `test users can load system resource URL`() = parentedSandbox {
        val executor = DeterministicSandboxExecutor<String, String?>(configuration)
        executor.run<GetSystemResourceURL>(manifestResourceName).apply {
            assertThat(result).endsWith("!/META-INF/MANIFEST.MF")
        }
    }

    class GetSystemResourceURL : Function<String, String?> {
        override fun apply(resourceName: String): String? {
            return ClassLoader.getSystemResource(resourceName)?.path
        }
    }

    @Test
    fun `test load user resources from system resource stream`() = parentedSandbox {
        val executor = DeterministicSandboxExecutor<String, ByteArray?>(configuration)
        executor.run<GetSystemResourceStream>(manifestResourceName).apply {
            assertThat(result).isNotEmpty()
        }
    }

    class GetSystemResourceStream : Function<String, ByteArray?> {
        override fun apply(resourceName: String): ByteArray? {
            return ClassLoader.getSystemResourceAsStream(resourceName)?.readBytes()
        }
    }
}