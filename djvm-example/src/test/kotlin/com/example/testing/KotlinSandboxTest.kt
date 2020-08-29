package com.example.testing

import com.example.testing.SandboxType.KOTLIN
import net.corda.djvm.api.RuleViolationError
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class KotlinSandboxTest : TestBase(KOTLIN) {
    @Test
    fun testGoodTask() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val output = taskFactory.create(KotlinTask::class.java).apply("Hello World!")
        assertEquals("Sandbox says: 'Hello World!'", output)
    }

    @Test
    fun testBadTask() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val exception = assertThrows<RuleViolationError> {
            taskFactory.create(BadKotlinTask::class.java).apply("field")
        }
        assertThat(exception)
            .hasMessageContaining("Disallowed reference to API; java.lang.Class.getDeclaredField(String)")
    }
}