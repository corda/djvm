package net.corda.djvm.execution

import net.corda.djvm.SandboxType.JAVA
import net.corda.djvm.TestBase
import net.corda.djvm.rewiring.SandboxClassLoadingException
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.function.Function

class KotlinNeedsKotlinTest : TestBase(JAVA) {
    @Test
    fun `kotlin code needs kotlin libraries`() = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<String, String>(configuration)
        val exception = assertThrows<SandboxException> {
            contractExecutor.run<UseKotlinForSomething>("Hello Kotlin!")
        }
        assertThat(exception)
            .hasCauseExactlyInstanceOf(SandboxClassLoadingException::class.java)
            .hasMessageContaining("Class file not found; kotlin/jvm/internal/Intrinsics.class")
    }

    class UseKotlinForSomething : Function<String, String> {
        override fun apply(input: String): String {
            return input
        }
    }
}
