package net.corda.djvm

import net.corda.djvm.SandboxType.KOTLIN
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.reflect.InvocationTargetException
import java.security.PrivilegedActionException
import java.util.function.Function

/**
 * Check that exception types thrown by the JVM itself
 * are marshalled correctly out of the sandbox.
 */
class JVMExceptionTest : TestBase(KOTLIN) {
    companion object {
        const val MESSAGE = "Hello World!"
        const val OTHER = "Boo!"
    }

    @Test
    fun testInvocationTargetException() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val throwMeTask = taskFactory.create(ThrowMeTask::class.java)
        val ex = assertThrows<InvocationTargetException> {
            throwMeTask.apply(InvocationTargetException(MyCustomException(OTHER), MESSAGE))
        }
        assertThat(ex)
            .hasCauseExactlyInstanceOf(Exception::class.java)
            .hasMessage(MESSAGE)
        assertThat(ex.cause)
            .hasMessage("sandbox.net.corda.djvm.MyCustomException -> Boo!")
            .hasNoCause()
    }

    @Test
    fun testExceptionInInitializerErrorWithThrowable() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val throwMeTask = taskFactory.create(ThrowMeTask::class.java)
        val ex = assertThrows<ExceptionInInitializerError> {
            throwMeTask.apply(ExceptionInInitializerError(MyCustomException(MESSAGE)))
        }
        assertThat(ex)
            .hasCauseExactlyInstanceOf(Exception::class.java)
            .hasMessage(null)
        assertThat(ex.cause)
            .hasMessage("sandbox.net.corda.djvm.MyCustomException -> Hello World!")
            .hasNoCause()
    }

    @Test
    fun testExceptionInInitializerErrorWithMessage() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val throwMeTask = taskFactory.create(ThrowMeTask::class.java)
        val ex = assertThrows<ExceptionInInitializerError> {
            throwMeTask.apply(ExceptionInInitializerError(MESSAGE))
        }
        assertThat(ex)
            .hasMessage(MESSAGE)
            .hasNoCause()
    }

    @Test
    fun testNoSuchMethodException() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val throwMeTask = taskFactory.create(ThrowMeTask::class.java)
        val ex = assertThrows<NoSuchMethodException> {
            throwMeTask.apply(NoSuchMethodException(MESSAGE))
        }
        assertThat(ex)
            .hasMessage(MESSAGE)
            .hasNoCause()
    }

    @Test
    fun testClassNotFoundException() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val throwMeTask = taskFactory.create(ThrowMeTask::class.java)
        val ex = assertThrows<ClassNotFoundException> {
            throwMeTask.apply(ClassNotFoundException(MESSAGE, MyCustomException(OTHER)))
        }
        assertThat(ex)
            .hasCauseExactlyInstanceOf(Exception::class.java)
            .hasMessage(MESSAGE)
        assertThat(ex.cause)
            .hasMessage("sandbox.net.corda.djvm.MyCustomException -> Boo!")
            .hasNoCause()
    }

    @Test
    fun testPrivilegedActionException() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val throwMeTask = taskFactory.create(ThrowMeTask::class.java)
        val ex = assertThrows<PrivilegedActionException> {
            throwMeTask.apply(PrivilegedActionException(MyCustomException(MESSAGE)))
        }
        assertThat(ex)
            .hasCauseExactlyInstanceOf(Exception::class.java)
            .hasMessage(null)
        assertThat(ex.cause)
            .hasMessage("sandbox.net.corda.djvm.MyCustomException -> Hello World!")
            .hasNoCause()
    }

    class ThrowMeTask : Function<Throwable, String> {
        override fun apply(t: Throwable): String {
            throw t
        }
    }
}

class MyCustomException(message: String) : Exception(message)
