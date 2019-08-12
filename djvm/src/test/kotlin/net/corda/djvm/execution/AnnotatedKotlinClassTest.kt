package net.corda.djvm.execution

import net.corda.djvm.KotlinAnnotation
import net.corda.djvm.SandboxType.*
import net.corda.djvm.TestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.util.function.Function
import kotlin.reflect.full.findAnnotation

class AnnotatedKotlinClassTest : TestBase(KOTLIN) {
    // The @kotlin.Metadata annotation is unavailable for Kotlin < 1.3.
    @Suppress("unchecked_cast")
    private val kotlinMetadata: Class<out Annotation> = Class.forName("kotlin.Metadata") as Class<out Annotation>

    @Test
    fun testSandboxAnnotation() = parentedSandbox {
        assertThat(UserKotlinData::class.findAnnotation<KotlinAnnotation>()).isNotNull

        @Suppress("unchecked_cast")
        val sandboxAnnotation = loadClass<KotlinAnnotation>().type as Class<out Annotation>
        val sandboxClass = loadClass<UserKotlinData>().type

        val annotationValue = sandboxClass.getAnnotation(sandboxAnnotation)
        assertThat(annotationValue.toString())
            .isEqualTo("@sandbox.net.corda.djvm.KotlinAnnotation(value=Hello Kotlin!)")
        assertThat(sandboxClass.kotlin.annotations).contains(annotationValue)
    }

    @Disabled("This test needs java.lang.Class.getEnclosingMethod() inside the sandbox.")
    @Test
    fun testAnnotationInsideSandbox() = parentedSandbox {
        val executor = DeterministicSandboxExecutor<Any?, String>(configuration)
        executor.run<ReadAnnotation>(null).apply {
            assertThat(result)
                .isEqualTo("@sandbox.net.corda.djvm.KotlinAnnotation(value=Hello Kotlin!)")
        }
    }

    class ReadAnnotation : Function<Any?, String> {
        override fun apply(t: Any?): String {
            return UserKotlinData::class.findAnnotation<KotlinAnnotation>().toString()
        }
    }

    @Test
    fun testPreservingKotlinMetadataAnnotation() = parentedSandbox(setOf(kotlinMetadata) ) {
        val sandboxClass = loadClass<UserKotlinData>().type
        @Suppress("unchecked_cast")
        val sandboxMetadataClass = loadClass(kotlinMetadata.name).type as Class<out Annotation>

        val metadata = sandboxClass.getAnnotation(kotlinMetadata)
        val sandboxMetadata = sandboxClass.getAnnotation(sandboxMetadataClass)

        with(AnnotationAssert(kotlinMetadata, sandboxMetadataClass)) {
            assertAll(
                { assertPropertyEquals("k", metadata, sandboxMetadata) },
                { assertPropertyEquals("bv", metadata, sandboxMetadata) },
                { assertPropertyEquals("mv", metadata, sandboxMetadata) },
                { assertPropertyEquals("d1", metadata, sandboxMetadata) },
                { assertPropertyEquals("d2", metadata, sandboxMetadata) },
                { assertPropertyEquals("pn", metadata, sandboxMetadata) },
                { assertPropertyEquals("xi", metadata, sandboxMetadata) },
                { assertPropertyEquals("xs", metadata, sandboxMetadata) }
            )
        }
    }

    class AnnotationAssert(private val type1: Class<out Annotation>, private val type2: Class<out Annotation>) {
        fun assertPropertyEquals(methodName: String, annotation1: Annotation, annotation2: Annotation) {
            val value1 = type1.getDeclaredMethod(methodName).invoke(annotation1)
            val value2 = type2.getDeclaredMethod(methodName).invoke(annotation2)
            when (value1) {
                is ByteArray -> assertArrayEquals(value1, value2 as ByteArray)
                is IntArray -> assertArrayEquals(value1, value2 as IntArray)
                is Array<*> -> assertArrayEquals(value1, value2 as Array<*>)
                else -> assertEquals(value1, value2)
            }
        }
    }
}

@KotlinAnnotation("Hello Kotlin!")
class UserKotlinData
