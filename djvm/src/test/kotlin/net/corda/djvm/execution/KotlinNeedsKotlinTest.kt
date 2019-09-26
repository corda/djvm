package net.corda.djvm.execution

import net.corda.djvm.SandboxType.JAVA
import net.corda.djvm.TestBase
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.function.Function

class KotlinNeedsKotlinTest : TestBase(JAVA) {
    @Test
    fun `kotlin code needs kotlin libraries`() {
        val exception = assertThrows<NoClassDefFoundError> {
            sandbox {
                val taskFactory = classLoader.createTaskFactory()
                classLoader.typedTaskFor<String, String, UseKotlinForSomething>(taskFactory)
                    .apply("Hello Kotlin!")
            }
        }
        assertThat(exception)
            .hasMessageContaining("sandbox/kotlin/jvm/internal/Intrinsics")
            .hasCauseExactlyInstanceOf(ClassNotFoundException::class.java)
        assertThat(exception.cause)
            .hasMessageContaining("Class file not found: kotlin/jvm/internal/Intrinsics.class")
    }

    class UseKotlinForSomething : Function<String, String> {
        override fun apply(input: String): String {
            return input
        }
    }
}
