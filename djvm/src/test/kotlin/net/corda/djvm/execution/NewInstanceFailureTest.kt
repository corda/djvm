package net.corda.djvm.execution

import com.example.testing.*
import net.corda.djvm.SandboxType.KOTLIN
import net.corda.djvm.TestBase
import net.corda.djvm.Utilities.newInstance
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.FileNotFoundException
import java.lang.reflect.InvocationTargetException
import java.util.function.Function

class NewInstanceFailureTest : TestBase(KOTLIN) {
    @Test
    fun `new instance permission failure`() = sandbox {
        val taskFactory = classLoader.createTaskFactory()
        val newInstance = classLoader.typedTaskFor(taskFactory, FailingNewInstance::class.java)
        val t = assertThrows<RuntimeException> { newInstance.apply(HasProtectedConstructor::class.java.name) }
        assertThat(t)
            .hasCauseExactlyInstanceOf(IllegalAccessException::class.java)
            .hasMessageStartingWith("sandbox.net.corda.djvm.execution.ExpectedException -> REFLECTION,sandbox.java.lang.IllegalAccessException,")
            .hasMessageContaining(HasProtectedConstructor::class.java.name)
            .hasMessageContaining("protected strictfp")
    }

    @Test
    fun `constructor new instance permission failure`() = sandbox {
        val taskFactory = classLoader.createTaskFactory()
        val newInstance = classLoader.typedTaskFor(taskFactory, FailingConstructorNewInstance::class.java)
        val t = assertThrows<RuntimeException> { newInstance.apply(HasProtectedConstructor::class.java.name) }
        assertThat(t)
            .hasCauseExactlyInstanceOf(IllegalAccessException::class.java)
            .hasMessageStartingWith("sandbox.net.corda.djvm.execution.ExpectedException -> REFLECTION,sandbox.java.lang.IllegalAccessException,")
            .hasMessageContaining(HasProtectedConstructor::class.java.name)
            .hasMessageContaining("protected strictfp")
    }

    @Test
    fun `new instance constructor failure`() = sandbox {
        val taskFactory = classLoader.createTaskFactory()
        val newInstance = classLoader.typedTaskFor(taskFactory, FailingNewInstance::class.java)
        val t = assertThrows<RuntimeException> { newInstance.apply(HasBrokenConstructor::class.java.name) }
        assertThat(t)
            .hasCauseExactlyInstanceOf(ArithmeticException::class.java)
            .hasMessage("sandbox.net.corda.djvm.execution.ExpectedException -> sandbox.java.lang.ArithmeticException,integer overflow")
    }

    @Test
    fun `constructor new instance constructor failure`() = sandbox {
        val taskFactory = classLoader.createTaskFactory()
        val newInstance = classLoader.typedTaskFor(taskFactory, FailingConstructorNewInstance::class.java)
        val t = assertThrows<RuntimeException> { newInstance.apply(HasBrokenConstructor::class.java.name) }
        assertThat(t)
            .hasCauseExactlyInstanceOf(ArithmeticException::class.java)
            .hasMessage("sandbox.net.corda.djvm.execution.ExpectedException -> INVOCATION-TARGET,sandbox.java.lang.ArithmeticException,integer overflow")
    }

    @Test
    fun `new instance instantiate failure`() = sandbox {
        val taskFactory = classLoader.createTaskFactory()
        val newInstance = classLoader.typedTaskFor(taskFactory, FailingNewInstance::class.java)
        val t = assertThrows<RuntimeException> { newInstance.apply(ImpossibleInstance::class.java.name) }
        assertThat(t)
            .hasCauseExactlyInstanceOf(InstantiationException::class.java)
            .hasMessageStartingWith("sandbox.net.corda.djvm.execution.ExpectedException -> REFLECTION,sandbox.java.lang.InstantiationException,")
    }

    @Test
    fun `constructor new instance instantiate failure`() = sandbox {
        val taskFactory = classLoader.createTaskFactory()
        val newInstance = classLoader.typedTaskFor(taskFactory, FailingConstructorNewInstance::class.java)
        val t = assertThrows<RuntimeException> { newInstance.apply(ImpossibleInstance::class.java.name) }
        assertThat(t)
            .hasCauseExactlyInstanceOf(InstantiationException::class.java)
            .hasMessageStartingWith("sandbox.net.corda.djvm.execution.ExpectedException -> REFLECTION,sandbox.java.lang.InstantiationException,")
    }

    @Test
    fun `new instance user failure`() = sandbox {
        val taskFactory = classLoader.createTaskFactory()
        val newInstance = classLoader.typedTaskFor(taskFactory, FailingNewInstance::class.java)
        val t = assertThrows<Exception> { newInstance.apply(HasUserExceptionConstructor::class.java.name) }
        assertThat(t)
            .hasCauseExactlyInstanceOf(Exception::class.java)
            .hasMessage("sandbox.net.corda.djvm.execution.ExpectedException -> FILE-NOT-FOUND,sandbox.java.io.FileNotFoundException,missing.dat")
    }

    @Test
    fun `constructor new instance user failure`() = sandbox {
        val taskFactory = classLoader.createTaskFactory()
        val newInstance = classLoader.typedTaskFor(taskFactory, FailingConstructorNewInstance::class.java)
        val t = assertThrows<Exception> { newInstance.apply(HasUserExceptionConstructor::class.java.name) }
        assertThat(t)
            .hasCauseExactlyInstanceOf(Exception::class.java)
            .hasMessage("sandbox.net.corda.djvm.execution.ExpectedException -> INVOCATION-TARGET,sandbox.java.io.FileNotFoundException,missing.dat")
    }

    @Test
    fun `throwing invocation target exception`() = sandbox {
        val taskFactory = classLoader.createTaskFactory()
        val newInstance = classLoader.typedTaskFor(taskFactory, ConstructNewInstance::class.java)
        val t = assertThrows<InvocationTargetException> { newInstance.apply(HasUserExceptionConstructor::class.java.name) }
        assertThat(t.cause)
                .isExactlyInstanceOf(Exception::class.java)
                .hasMessage("sandbox.java.io.FileNotFoundException -> missing.dat")
    }

    @Test
    fun `throwing exception in initializer`() = sandbox {
        val taskFactory = classLoader.createTaskFactory()
        val newInstance = classLoader.typedTaskFor(taskFactory, ConstructNewInstance::class.java)
        val t = assertThrows<ExceptionInInitializerError> { newInstance.apply(HasBrokenInitializer::class.java.name) }
        assertThat(t.cause)
            .isExactlyInstanceOf(RuntimeException::class.java)
            .hasMessage("sandbox.java.io.UncheckedIOException -> sandbox.java.io.FileNotFoundException: missing.dat")
    }

    class FailingNewInstance : Function<String, String?> {
        override fun apply(className: String): String? {
            try {
                return javaClass.classLoader.loadClass(className).newInstance().toString()
            } catch (e: InvocationTargetException) {
                val target = e.cause!!
                throw ExpectedException("INVOCATION-TARGET,${target::class.java.name},${target.message}", target)
            } catch (e: ReflectiveOperationException) {
                throw ExpectedException("REFLECTION,${e::class.java.name},${e.message}", e)
            } catch (e: FileNotFoundException) {
                throw ExpectedException("FILE-NOT-FOUND,${e::class.java.name},${e.message}", e)
            } catch (e: Exception) {
                throw ExpectedException("${e::class.java.name},${e.message}", e)
            }
        }
    }

    class FailingConstructorNewInstance : Function<String, String?> {
        override fun apply(className: String): String? {
            try {
                return newInstance(javaClass.classLoader.loadClass(className)).toString()
            } catch (e: InvocationTargetException) {
                val target = e.cause!!
                throw ExpectedException("INVOCATION-TARGET,${target::class.java.name},${target.message}", target)
            } catch (e: ReflectiveOperationException) {
                throw ExpectedException("REFLECTION,${e::class.java.name},${e.message}", e)
            } catch (e: FileNotFoundException) {
                throw ExpectedException("FILE-NOT-FOUND,${e::class.java.name},${e.message}", e)
            } catch (e: Exception) {
                throw ExpectedException("${e::class.java.name},${e.message}", e)
            }
        }
    }

    class ConstructNewInstance : Function<String, String?> {
        override fun apply(className: String): String? {
            return newInstance(javaClass.classLoader.loadClass(className)).toString()
        }
    }
}
