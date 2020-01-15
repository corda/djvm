package net.corda.djvm.execution

import net.corda.djvm.SandboxType.KOTLIN
import net.corda.djvm.TestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.function.Function

class SandboxThrowableTest : TestBase(KOTLIN) {

    @Test
    fun `test user exception handling`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val result = taskFactory.create(ThrowAndCatchExample::class.java)
            .apply("Hello World")
        assertThat(result)
            .isEqualTo(arrayOf("FIRST FINALLY", "BASE EXCEPTION", "Hello World", "SECOND FINALLY"))
    }

    @Test
    fun `test rethrowing an exception`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val result = taskFactory.create(ThrowAndRethrowExample::class.java)
            .apply("Hello World")
        assertThat(result)
            .isEqualTo(arrayOf("FIRST CATCH", "FIRST FINALLY", "SECOND CATCH", "Hello World", "SECOND FINALLY"))
    }

    @Test
    fun `test JVM exceptions still propagate`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val result = taskFactory.create(TriggerJVMException::class.java)
            .apply(-1)
        assertThat(result)
            .startsWith("sandbox.java.lang.ArrayIndexOutOfBoundsException:")
            .matches(".*:(-1|Index -1 out of bounds for length 3)+\$")
    }
}

class ThrowAndRethrowExample : Function<String, Array<String>> {
    override fun apply(input: String): Array<String> {
        val data = mutableListOf<String>()
        try {
            try {
                throw MyExampleException(input)
            } catch (e: Exception) {
                data += "FIRST CATCH"
                throw e
            } finally {
                data += "FIRST FINALLY"
            }
        } catch (e: MyExampleException) {
            data += "SECOND CATCH"
            e.message?.apply { data += this }
        } finally {
            data += "SECOND FINALLY"
        }

        return data.toTypedArray()
    }
}

class ThrowAndCatchExample : Function<String, Array<String>> {
    override fun apply(input: String): Array<String> {
        val data = mutableListOf<String>()
        try {
            try {
                throw MyExampleException(input)
            } finally {
                data += "FIRST FINALLY"
            }
        } catch (e: MyBaseException) {
            data += "BASE EXCEPTION"
            e.message?.apply { data += this }
        } catch (e: Exception) {
            data += "NOT THIS ONE!"
        } finally {
            data += "SECOND FINALLY"
        }

        return data.toTypedArray()
    }
}

class TriggerJVMException : Function<Int, String> {
    override fun apply(input: Int): String {
        return try {
            arrayOf(0, 1, 2)[input]
            "No Error"
        } catch (e: Exception) {
            e.javaClass.name + ':' + (e.message ?: "<MESSAGE MISSING>")
        }
    }
}
