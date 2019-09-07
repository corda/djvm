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
    fun `kotlin code needs kotlin libraries`() {
        val exception = assertThrows<SandboxClassLoadingException> {
            parentedSandbox {
                val executor = classLoader.createExecutor()
                classLoader.typedTaskFor<String, String, UseKotlinForSomething>(executor)
                    .apply("Hello Kotlin!")
            }
        }
        assertThat(exception)
            .isExactlyInstanceOf(SandboxClassLoadingException::class.java)
            .hasMessageContaining("Class file not found: kotlin/jvm/internal/Intrinsics.class")
    }

    class UseKotlinForSomething : Function<String, String> {
        override fun apply(input: String): String {
            return input
        }
    }
}
