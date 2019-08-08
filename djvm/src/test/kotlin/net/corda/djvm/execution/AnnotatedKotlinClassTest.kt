package net.corda.djvm.execution

import net.corda.djvm.KotlinAnnotation
import net.corda.djvm.SandboxType.*
import net.corda.djvm.TestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.function.Function
import kotlin.reflect.full.findAnnotation

class AnnotatedKotlinClassTest : TestBase(KOTLIN) {
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
}

@KotlinAnnotation("Hello Kotlin!")
class UserKotlinData
