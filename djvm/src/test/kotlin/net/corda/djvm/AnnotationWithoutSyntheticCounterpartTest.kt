package net.corda.djvm

import net.corda.djvm.SandboxType.KOTLIN
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * Check that the DJVM does not create synthetic annotation
 * counterparts for these annotations, because we're going
 * to use the JVM's own [Annotation] classes here.
 */
class AnnotationWithoutSyntheticCounterpartTest : TestBase(KOTLIN) {
    @ParameterizedTest(name = "[{index}] => {0}")
    @ValueSource(strings = [
        "sandbox.java.lang.FunctionalInterface",
        "sandbox.java.lang.annotation.Documented",
        "sandbox.java.lang.annotation.Inherited",
        "sandbox.kotlin.annotation.MustBeDocumented"
    ])
    fun testNoSyntheticAnnotationForJVM(annotationName: String) = sandbox {
        val syntheticName = "$annotationName\$1DJVM"
        val ex = assertThrows<ClassNotFoundException> { classLoader.loadClass(syntheticName) }
        assertThat(ex).hasMessageContaining(syntheticName)
        assertDoesNotThrow { classLoader.loadClass(annotationName) }

        // Check we can reload this class, to prove it has already been loaded correctly!
        assertDoesNotThrow { classLoader.loadClass(annotationName) }
    }
}