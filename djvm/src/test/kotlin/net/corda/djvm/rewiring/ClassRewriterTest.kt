package net.corda.djvm.rewiring

import foo.bar.sandbox.A
import foo.bar.sandbox.B
import foo.bar.sandbox.Empty
import foo.bar.sandbox.StrictFloat
import net.corda.djvm.SandboxType.KOTLIN
import net.corda.djvm.TestBase
import net.corda.djvm.api.RuleViolationError
import net.corda.djvm.assertions.AssertionExtensions.assertThat
import net.corda.djvm.costing.ThresholdViolationError
import net.corda.djvm.execution.ExecutionProfile
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import java.util.Arrays

class ClassRewriterTest : TestBase(KOTLIN) {

    @Test
    fun `empty transformer does nothing`() = customSandbox(BLANK, enableTracing = false) {
        // An empty transformer doesn't contain the tracing emitters either.
        val callable = newCallable<Empty>()
        assertThat(callable).isSandboxed()
        callable.createAndInvoke()
        assertThat(runtimeCosts).areZero()
    }

    @Test
    fun `can transform class`() = sandbox {
        val callable = newCallable<A>()
        assertThat(callable).hasBeenModified()
        callable.createAndInvoke()
        assertThat(runtimeCosts).hasInvocationCost(1)
    }

    @Test
    fun `can transform another class`() = sandbox {
        val callable = newCallable<B>()
        assertThat(callable).hasBeenModified()
        assertThat(callable).isSandboxed()
        callable.createAndInvoke()
        assertThat(runtimeCosts)
            .hasInvocationCostGreaterThanOrEqualTo(1) // Includes static constructor calls for java.lang.Math, etc.
            .hasJumpCostGreaterThanOrEqualTo(30 * 2 + 1)
    }

    @Test
    fun `cannot breach threshold`() = customSandbox(ExecutionProfile.DISABLE_BRANCHING, DEFAULT) {
        val callable = newCallable<B>()
        assertThat(callable).hasBeenModified()
        assertThat(callable).isSandboxed()
        assertThatExceptionOfType(ThresholdViolationError::class.java).isThrownBy {
            callable.createAndInvoke()
        }.withMessageContaining("terminated due to excessive use of looping")
        assertThat(runtimeCosts)
            .hasAllocationCost(0)
            .hasInvocationCost(1)
            .hasJumpCost(1)
    }

    @Test
    fun `can transform class into using strictfp`() = sandbox {
        val callable = newCallable<StrictFloat>()
        assertThat(callable).hasBeenModified()
        callable.createAndInvoke()
    }

    @Test
    fun `can load a Java API that still exists in Java runtime`() = sandbox {
        assertThat(loadClass<MutableList<*>>())
            .hasClassName("sandbox.java.util.List")
            .hasBeenModified()
    }

    @Test
    fun `cannot load a Java API that was deleted from Java runtime`() = sandbox {
        assertThatExceptionOfType(ClassNotFoundException::class.java)
            .isThrownBy { loadClass<Paths>() }
            .withMessageContaining("Class file not found: java/nio/file/Paths.class")
    }

    @Test
    fun `load internal Sun class that still exists in Java runtime`() = sandbox {
        assertThat(loadClass<sun.misc.Unsafe>())
            .hasClassName("sandbox.sun.misc.Unsafe")
            .hasBeenModified()
    }

    @Test
    fun `cannot load internal Sun class that was deleted from Java runtime`() = sandbox {
        assertThatExceptionOfType(ClassNotFoundException::class.java)
            .isThrownBy { loadClass("sun.misc.Timer") }
            .withMessageContaining("Class file not found: sun/misc/Timer.class")
    }

    @Test
    fun `can load local class`() = sandbox {
        assertThat(loadClass<Example>())
            .hasClassName("sandbox.net.corda.djvm.rewiring.ClassRewriterTest\$Example")
            .hasBeenModified()
    }

    class Example : java.util.function.Function<Int, Int> {
        override fun apply(input: Int): Int {
            return input
        }
    }

    @Test
    fun `can load class with constant fields`() = sandbox {
        assertThat(loadClass<ObjectWithConstants>())
            .hasClassName("sandbox.net.corda.djvm.rewiring.ObjectWithConstants")
            .hasBeenModified()
    }

    @Test
    fun `test rewrite static method`() = sandbox {
        assertThat(loadClass<Arrays>())
            .hasClassName("sandbox.java.util.Arrays")
            .hasBeenModified()
    }

    @Test
    fun `test stitch new super-interface`() = sandbox {
        assertThat(loadClass<CharSequence>())
            .hasClassName("sandbox.java.lang.CharSequence")
            .hasInterface("java.lang.CharSequence")
            .hasBeenModified()
    }

    @Test
    fun `test class with stitched interface`() = sandbox {
        assertThat(loadClass<StringBuilder>())
            .hasClassName("sandbox.java.lang.StringBuilder")
            .hasInterface("sandbox.java.lang.CharSequence")
            .hasBeenModified()
    }

    @Test
    fun `test Java class is owned by parent classloader`() = sandbox {
        val stringBuilderClass = loadClass<StringBuilder>().type
        assertThat(stringBuilderClass.classLoader).isEqualTo(classLoader.parent)
    }

    @Test
    fun `test user class is owned by new classloader`() = sandbox {
        assertThat(loadClass<Empty>())
            .hasClassLoader(classLoader)
            .hasBeenModified()
    }

    @Test
    fun `test template class is owned by parent classloader`() = sandbox {
        assertThat(classLoader.loadForSandbox("sandbox.java.lang.DJVM"))
            .hasClassLoader(classLoader.parent)
            .hasNotBeenModified()
    }

    @Test
    fun `test rule violation error cannot be loaded`() = sandbox {
        assertThatExceptionOfType(ClassNotFoundException::class.java)
            .isThrownBy { loadClass<RuleViolationError>() }
            .withMessageContaining("Class file not found: net/corda/djvm/api/RuleViolationError.class")
    }

    @Test
    fun `test threshold violation error cannot be loaded`() = sandbox {
        assertThatExceptionOfType(ClassNotFoundException::class.java)
            .isThrownBy { loadClass<ThresholdViolationError>() }
            .withMessageContaining("Class file not found: net/corda/djvm/costing/ThresholdViolationError.class")
    }
}

@Suppress("unused")
private object ObjectWithConstants {
    const val MESSAGE = "Hello Sandbox!"
    const val BIG_NUMBER = 99999L
    const val NUMBER = 100
    const val CHAR = '?'
    const val BYTE = 7f.toInt().toByte()
    val DATA = emptyArray<String>()
}
