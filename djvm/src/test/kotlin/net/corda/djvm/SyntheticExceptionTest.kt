package net.corda.djvm

import net.corda.djvm.SandboxType.KOTLIN
import net.corda.djvm.rewiring.SandboxClassLoader
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

/**
 * Check that synthetic throwable classes are created in the
 * same [ClassLoader] as their [sandbox.java.lang.Throwable]
 * counterparts.
 */
class SyntheticExceptionTest : TestBase(KOTLIN) {
    companion object {
        const val PARENT_CLASSLOADER_EXCEPTION = "sandbox.java.security.NoSuchAlgorithmException"
        const val CHILD_CLASSLOADER_EXCEPTION = "sandbox.net.corda.djvm.execution.MyExampleException"
    }

    @Test
    fun testSyntheticJavaExceptionIsCreatedInCorrectClassLoader() = sandbox {
        assertThat(classLoader.parent).isInstanceOf(SandboxClassLoader::class.java)
        flushInternalCache()

        val syntheticClassName = "$PARENT_CLASSLOADER_EXCEPTION\$1DJVM"
        val syntheticClass = classLoader.loadClass(syntheticClassName).asSubclass(Throwable::class.java)
        val exceptionClass = classLoader.loadClass(PARENT_CLASSLOADER_EXCEPTION)
        assertThat(syntheticClass.classLoader).isSameAs(exceptionClass.classLoader)
        assertThat(syntheticClass.classLoader).isSameAs(classLoader.parent)

        // Check we can reload this class, to prove it has already been loaded correctly!
        assertDoesNotThrow { classLoader.loadClass(syntheticClassName) }
    }

    @Test
    fun testSyntheticUserExceptionIsCreatedInCorrectClassLoader() = sandbox {
        assertThat(classLoader.parent).isInstanceOf(SandboxClassLoader::class.java)
        flushInternalCache()

        val syntheticClassName = "$CHILD_CLASSLOADER_EXCEPTION\$1DJVM"
        val syntheticClass = classLoader.loadClass(syntheticClassName).asSubclass(Throwable::class.java)
        val exceptionClass = classLoader.loadClass(CHILD_CLASSLOADER_EXCEPTION)
        assertThat(syntheticClass.classLoader).isSameAs(exceptionClass.classLoader)
        assertThat(syntheticClass.classLoader).isSameAs(classLoader)

        // Check we can reload this class, to prove it has already been loaded correctly!
        assertDoesNotThrow { classLoader.loadClass(syntheticClassName) }
    }
}
