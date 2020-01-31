package net.corda.djvm

import net.corda.djvm.SandboxType.KOTLIN
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * Check that the DJVM does not create synthetic throwable
 * counterparts for these exceptions, because we're going
 * to use the JVM's own [Throwable] classes here.
 *
 * This list is representative rather than exhaustive.
 */
class ExceptionWithoutSyntheticCounterpartTest : TestBase(KOTLIN) {
    @ParameterizedTest(name = "[{index}] => {0}")
    @ValueSource(strings = [
        "sandbox.java.lang.Throwable",
        "sandbox.java.lang.Exception",
        "sandbox.java.lang.RuntimeException",
        "sandbox.java.lang.Error",
        "sandbox.java.lang.LinkageError",
        "sandbox.java.lang.ThreadDeath",
        "sandbox.java.lang.VirtualMachineError",
        "sandbox.java.lang.DJVMThrowableWrapper"
    ])
    fun testNoSyntheticExceptionForJVM(exceptionName: String) = sandbox {
        val syntheticName = "$exceptionName\$1DJVM"
        val ex = assertThrows<ClassNotFoundException> { classLoader.loadClass(syntheticName) }
        assertThat(ex).hasMessageContaining(syntheticName)
        assertDoesNotThrow { classLoader.loadClass(exceptionName) }

        // Check we can reload this class, to prove it has already been loaded correctly!
        assertDoesNotThrow { classLoader.loadClass(exceptionName) }
    }
}