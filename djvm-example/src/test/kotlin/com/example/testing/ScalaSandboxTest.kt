package com.example.testing

import com.example.testing.SandboxType.KOTLIN
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
        val taskFactory = classLoader.createTypedTaskFactory()
        val output = taskFactory.create(ScalaTask::class.java).apply(0xbadf00d)
        assertEquals("Sandbox says: 'BADF00D'", output)
    }

    @Test
    fun testBadTask() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val ex = assertThrows<RuleViolationError> {
            taskFactory.create(BadScalaTask::class.java).apply("Secret Message!".toByteArray())
        }
        assertThat(ex)
            .hasMessageContaining("Disallowed reference to API; java.lang.invoke.MethodHandleImpl\$1.run()")
    }
}
