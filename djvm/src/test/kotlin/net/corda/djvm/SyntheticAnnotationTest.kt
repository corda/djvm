package net.corda.djvm

import net.corda.djvm.SandboxType.KOTLIN
import net.corda.djvm.rewiring.SandboxClassLoader
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

/**
 * Check that synthetic annotation classes are created in the same
 * [ClassLoader] as their [sandbox.java.lang.annotation.Annotation]
 * counterparts.
 */
class SyntheticAnnotationTest : TestBase(KOTLIN) {
    companion object {
        const val PARENT_CLASSLOADER_ANNOTATION = "sandbox.java.lang.annotation.Retention"
        const val CHILD_CLASSLOADER_ANNOTATION = "sandbox.net.corda.djvm.KotlinAnnotation"
    }

    @Test
    fun testSyntheticJavaAnnotationIsCreatedInCorrectClassLoader() = sandbox {
        assertThat(classLoader.parent).isInstanceOf(SandboxClassLoader::class.java)
        flushInternalCache()

        val syntheticClassName = "$PARENT_CLASSLOADER_ANNOTATION\$1DJVM"
        val syntheticClass = classLoader.loadClass(syntheticClassName).asSubclass(Annotation::class.java)
        val annotationClass = classLoader.loadClass(PARENT_CLASSLOADER_ANNOTATION)
        assertThat(syntheticClass.classLoader).isSameAs(annotationClass.classLoader)
        assertThat(syntheticClass.classLoader).isSameAs(classLoader.parent)

        // Check we can reload this class, to prove it has already been loaded correctly!
        assertDoesNotThrow { classLoader.loadClass(syntheticClassName) }
    }

    @Test
    fun testSyntheticUserAnnotationIsCreatedInCorrectClassLoader() = sandbox {
        assertThat(classLoader.parent).isInstanceOf(SandboxClassLoader::class.java)
        flushInternalCache()

        val syntheticClassName = "$CHILD_CLASSLOADER_ANNOTATION\$1DJVM"
        val syntheticClass = classLoader.loadClass(syntheticClassName).asSubclass(Annotation::class.java)
        val annotationClass = classLoader.loadClass(CHILD_CLASSLOADER_ANNOTATION)
        assertThat(syntheticClass.classLoader).isSameAs(annotationClass.classLoader)
        assertThat(syntheticClass.classLoader).isSameAs(classLoader)

        // Check we can reload this class, to prove it has already been loaded correctly!
        assertDoesNotThrow { classLoader.loadClass(syntheticClassName) }
    }
}
