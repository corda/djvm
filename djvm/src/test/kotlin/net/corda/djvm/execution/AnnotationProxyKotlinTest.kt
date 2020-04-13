package net.corda.djvm.execution

import net.corda.djvm.KotlinAnnotation
import net.corda.djvm.SandboxType.KOTLIN
import net.corda.djvm.TestBase
import net.corda.djvm.TypedTaskFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.function.Function
import kotlin.reflect.full.findAnnotation

class AnnotationProxyKotlinTest : TestBase(KOTLIN)  {
    @Disabled("This test needs Kotlin's BuiltInsLoader inside the sandbox, i.e. META-INF/services/")
    @Test
    fun testAnnotationInsideSandbox() = sandbox {
        val taskFactory: TypedTaskFactory = classLoader.createTypedTaskFactory()
        val result = taskFactory.create(ReadAnnotation::class.java).apply(null)
        assertThat(result)
            .matches("^\\Q@sandbox.net.corda.djvm.KotlinAnnotation(value=\\E\"?Hello Kotlin!\"?\\)\$")
    }

    class ReadAnnotation : Function<Any?, String> {
        override fun apply(t: Any?): String {
            return UserKotlinData::class.findAnnotation<KotlinAnnotation>().toString()
        }
    }
}