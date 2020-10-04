package net.corda.djvm.execution

import net.corda.djvm.SandboxType.KOTLIN
import net.corda.djvm.TestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.function.Consumer
import java.util.function.Function

class ResetKotlinObjectTest : TestBase(KOTLIN) {
    @Test
    fun testResetKotlinObject() = create {
        sandbox(this, Consumer {
            val taskFactory = classLoader.createTypedTaskFactory()
            val updateExampleObject = taskFactory.create(UpdateExampleObject::class.java)
            val firstResult = updateExampleObject.apply("Big Wide World!")
            assertThat(firstResult).isEqualTo("Hello Sandbox!")
            val secondResult = updateExampleObject.apply("")
            assertThat(secondResult).isEqualTo("Big Wide World!")
        })

        sandbox(this, Consumer {
            val taskFactory = classLoader.createTypedTaskFactory()
            val updateExampleObject = taskFactory.create(UpdateExampleObject::class.java)
            val thirdResult = updateExampleObject.apply("Brave New World!")
            assertThat(thirdResult).isEqualTo("Hello Sandbox!")
        })
    }

    object ExampleObject {
        @JvmField
        var message: String = "Hello Sandbox!"
    }

    class UpdateExampleObject : Function<String, String> {
        override fun apply(input: String): String {
            val result = ExampleObject.message
            ExampleObject.message = input
            return result
        }
    }
}