package net.corda.djvm

import net.corda.djvm.SandboxType.KOTLIN
import net.corda.djvm.assertions.AssertionExtensions.assertThatDJVM
import net.corda.djvm.rewiring.SandboxClassLoadingException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import java.util.function.Function
import java.util.*

class DJVMExceptionTest : TestBase(KOTLIN) {
    @Test
    fun testSingleException() = parentedSandbox {
        val executor = TaskExecutor(classLoader)
        val taskClass = executor.toSandboxClass(SingleExceptionTask::class.java)
        val result = executor.apply(taskClass.newInstance(), "Hello World")
        assertThat(result).isInstanceOf(Throwable::class.java)
        result as Throwable

        assertThat(result.message).isEqualTo("Hello World")
        assertThat(result.cause).isNull()
        assertThat(result.stackTrace)
            .hasSize(2)
            .allSatisfy { it is StackTraceElement && it.className == result.javaClass.name }
    }

    @Test
    fun testMultipleExceptions() = parentedSandbox {
        val executor = TaskExecutor(classLoader)
        val taskClass = executor.toSandboxClass(MultipleExceptionsTask::class.java)
        val result = executor.apply(taskClass.newInstance(), "Hello World")
        assertThat(result).isInstanceOf(Throwable::class.java)
        result as Throwable

        assertThat(result.message).isEqualTo("Hello World(1)(2)")
        assertThat(result.cause).isInstanceOf(Throwable::class.java)
        assertThat(result.stackTrace)
            .hasSize(2)
            .allSatisfy { it is StackTraceElement && it.className == result.javaClass.name }
        val resultLineNumbers = result.stackTrace.toLineNumbers()

        val firstCause = result.cause as Throwable
        assertThat(firstCause.message).isEqualTo("Hello World(1)")
        assertThat(firstCause.cause).isInstanceOf(Throwable::class.java)
        assertThat(firstCause.stackTrace)
            .hasSize(2)
            .allSatisfy { it is StackTraceElement && it.className == result.javaClass.name }
        val firstCauseLineNumbers = firstCause.stackTrace.toLineNumbers()

        val rootCause = firstCause.cause as Throwable
        assertThat(rootCause.message).isEqualTo("Hello World")
        assertThat(rootCause.cause).isNull()
        assertThat(rootCause.stackTrace)
            .hasSize(2)
            .allSatisfy { it is StackTraceElement && it.className == result.javaClass.name }
        val rootCauseLineNumbers = rootCause.stackTrace.toLineNumbers()

        // These stack traces should share one line number and have one distinct line number each.
        assertThat(resultLineNumbers.toSet() + firstCauseLineNumbers.toSet() + rootCauseLineNumbers.toSet())
            .hasSize(4)
    }

    @Test
    fun testJavaThrowableToSandbox() = parentedSandbox {
        val djvm = DJVM(classLoader)
        val helloWorld = djvm.stringOf("Hello World")

        val result = djvm.sandbox(Throwable("Hello World"))
        assertThatDJVM(result)
            .hasClassName("sandbox.java.lang.Throwable")
            .isAssignableFrom(djvm.throwableClass)
            .hasGetterValue("getMessage", helloWorld)
            .hasGetterNullValue("getCause")

        assertThat(result.getArray("getStackTrace"))
            .hasOnlyElementsOfType(djvm.stackTraceElementClass)
            .isNotEmpty
    }

    @Test
    fun testWeCreateCorrectJVMExceptionAtRuntime() = parentedSandbox {
        val djvm = DJVM(classLoader)
        val helloWorld = djvm.stringOf("Hello World")

        val result = djvm.sandbox(RuntimeException("Hello World"))
        assertThatDJVM(result)
            .hasClassName("sandbox.java.lang.RuntimeException")
            .isAssignableFrom(djvm.throwableClass)
            .hasGetterValue("getMessage", helloWorld)
            .hasGetterNullValue("getCause")

        assertThat(result.getArray("getStackTrace"))
            .hasOnlyElementsOfType(djvm.stackTraceElementClass)
            .isNotEmpty

        assertThatExceptionOfType(ClassNotFoundException::class.java)
            .isThrownBy { djvm.classFor("sandbox.java.lang.RuntimeException\$1DJVM") }
            .withMessage("sandbox.java.lang.RuntimeException\$1DJVM")
    }

    @Test
    fun testWeCreateCorrectSyntheticExceptionAtRuntime() = parentedSandbox {
        val djvm = DJVM(classLoader)

        val result = djvm.sandbox(EmptyStackException())
        assertThatDJVM(result)
            .hasClassName("sandbox.java.util.EmptyStackException")
            .isAssignableFrom(djvm.throwableClass)
            .hasGetterNullValue("getMessage")
            .hasGetterNullValue("getCause")

        assertThat(result.getArray("getStackTrace"))
            .hasOnlyElementsOfType(djvm.stackTraceElementClass)
            .isNotEmpty

        assertThatDJVM(djvm.classFor("sandbox.java.util.EmptyStackException\$1DJVM"))
            .isAssignableFrom(RuntimeException::class.java)
    }

    @Test
    fun testWeCannotCreateSyntheticExceptionForNonException() = parentedSandbox {
        val djvm = DJVM(classLoader)
        assertThatExceptionOfType(ClassNotFoundException::class.java)
            .isThrownBy { djvm.classFor("sandbox.java.util.LinkedList\$1DJVM") }
            .withMessage("sandbox.java.util.LinkedList\$1DJVM")
    }

    /**
     * This scenario should never happen in practice. We just need to be sure
     * that the classloader can handle it.
     */
    @Test
    fun testWeCannotCreateSyntheticExceptionForImaginaryJavaClass() = parentedSandbox {
        val djvm = DJVM(classLoader)
        assertThatExceptionOfType(SandboxClassLoadingException::class.java)
            .isThrownBy { djvm.classFor("sandbox.java.util.DoesNotExist\$1DJVM") }
            .withMessageContaining("Class file not found: java/util/DoesNotExist")
    }

    /**
     * This scenario should never happen in practice. We just need to be sure
     * that the classloader can handle it.
     */
    @Test
    fun testWeCannotCreateSyntheticExceptionForImaginaryUserClass() = parentedSandbox {
        val djvm = DJVM(classLoader)
        assertThatExceptionOfType(SandboxClassLoadingException::class.java)
            .isThrownBy { djvm.classFor("sandbox.com.example.DoesNotExist\$1DJVM") }
            .withMessageContaining("Class file not found: com/example/DoesNotExist")
    }
}

class SingleExceptionTask : Function<Any?, Throwable?> {
    override fun apply(input: Any?): Throwable? {
        return Throwable(input as? String)
    }
}

class MultipleExceptionsTask : Function<Any?, Throwable?> {
    override fun apply(input: Any?): Throwable? {
        val root = Throwable(input as? String)
        val nested = Throwable(root.message + "(1)", root)
        return Throwable(nested.message + "(2)", nested)
    }
}

private fun Array<StackTraceElement>.toLineNumbers(): IntArray {
    return map(StackTraceElement::getLineNumber).toIntArray()
}