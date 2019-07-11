package com.example.testing

import com.example.testing.SandboxType.KOTLIN
import net.corda.djvm.execution.DeterministicSandboxExecutor
import net.corda.djvm.execution.SandboxException
import net.corda.djvm.rules.RuleViolationError
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class KotlinSandboxTest : TestBase(KOTLIN) {
    @Test
    fun testGoodTask() = sandbox {
        val executor = DeterministicSandboxExecutor<String, String>(configuration)
        val output = executor.run<KotlinTask>("Hello World!").result
        assertEquals("Sandbox says: 'Hello World!'", output)
    }

    @Test
    fun testBadTask() = sandbox {
        val executor = DeterministicSandboxExecutor<String, Long>(configuration)
        val exception = assertThrows<SandboxException> { executor.run<BadKotlinTask>("field") }
        assertThat(exception)
            .hasCauseExactlyInstanceOf(RuleViolationError::class.java)
            .hasMessageContaining("Disallowed reference to API; java.lang.Class.getField(String)")
    }
}