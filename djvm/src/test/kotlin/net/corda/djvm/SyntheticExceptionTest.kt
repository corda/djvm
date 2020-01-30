package net.corda.djvm

import net.corda.djvm.SandboxType.KOTLIN
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Check that synthetic throwable classes are created in the
 * same [ClassLoader] as their [sandbox.java.lang.Throwable]
 * counterparts.
 */
class SyntheticExceptionTest : TestBase(KOTLIN) {
    @Test
    fun testSyntheticJavaExceptionIsCreatedInCorrectClassLoader() = sandbox {
        val syntheticClass = classLoader.loadClass("sandbox.java.security.NoSuchAlgorithmException\$1DJVM")
        val exceptionClass = classLoader.loadClass("sandbox.java.security.NoSuchAlgorithmException")
        assertThat(syntheticClass.classLoader).isSameAs(exceptionClass.classLoader)
        assertThat(syntheticClass.classLoader).isNotSameAs(classLoader)
    }

    @Test
    fun testSyntheticUserExceptionIsCreatedInCorrectClassLoader() = sandbox {
        val syntheticClass = classLoader.loadClass("sandbox.net.corda.djvm.execution.MyExampleException\$1DJVM")
        val exceptionClass = classLoader.loadClass("sandbox.net.corda.djvm.execution.MyExampleException")
        assertThat(syntheticClass.classLoader).isSameAs(exceptionClass.classLoader)
        assertThat(syntheticClass.classLoader).isSameAs(classLoader)
    }
}
