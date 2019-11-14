package com.example.testing

import com.example.testing.SandboxType.KOTLIN
import net.corda.djvm.execution.DeterministicSandboxExecutor
import net.corda.djvm.execution.SandboxException
import net.corda.djvm.rules.RuleViolationError
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Scala's runtime library doesn't play nicely inside the DJVM's sandbox.
 * The [java.lang.invoke] classes are currently off-limits!
 */
class ScalaSandboxTest : TestBase(KOTLIN) {
    @Test
    fun testGoodTask() = sandbox {
        val executor = DeterministicSandboxExecutor<Int, String>(configuration)
        val output = executor.run<ScalaTask>(0xbadf00d).result
        assertEquals("Sandbox says: 'BADF00D'", output)
    }

    @Test
    fun testBadTask() = sandbox {
        val executor = DeterministicSandboxExecutor<ByteArray, String>(configuration)
        val ex = assertThrows<SandboxException> {
            executor.run<BadScalaTask>("Secret Message!".toByteArray())
        }
        assertThat(ex)
            .hasCauseExactlyInstanceOf(RuleViolationError::class.java)
            .hasMessageContaining("Disallowed reference to reflection API; java.lang.invoke.MethodHandleImpl\$1.run()")
    }
}
