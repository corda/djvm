package net.corda.djvm.rules

import foo.bar.sandbox.Callable
import net.corda.djvm.SandboxType.KOTLIN
import net.corda.djvm.TestBase
import net.corda.djvm.assertions.AssertionExtensions.assertThat
import org.junit.jupiter.api.Test
import org.objectweb.asm.Type
import java.util.*

class ReferenceExtractorTest : TestBase(KOTLIN) {

    class A : Callable {
        override fun call() {
            synchronized(this) {
                Random().nextInt()
            }
        }
    }

    @Test
    fun `can find class references`() = validate<C> { context ->
        assertThat(context.references)
                .hasClass(Type.getInternalName(A::class.java))
    }

    class C {
        @JvmField
        val foo: A? = null

        fun test(): A? {
            return foo
        }
    }

}
