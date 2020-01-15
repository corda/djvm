package net.corda.djvm.execution

import net.corda.djvm.SandboxType.KOTLIN
import net.corda.djvm.TestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater
import java.util.concurrent.atomic.AtomicLongFieldUpdater
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater
import java.util.function.Function

class SandboxFieldUpdaterTest : TestBase(KOTLIN) {
    @Test
    fun `test long field updater`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val result = taskFactory.create(HasLongField::class.java)
            .apply(1234)
        assertThat(result).isEqualTo(1234)
    }

    class HasLongField : Function<Long, Long> {
        @Suppress("unused")
        @Volatile
        private var longField: Long = 0

        override fun apply(input: Long): Long {
            val updater = AtomicLongFieldUpdater.newUpdater(javaClass, "longField")
            return updater.addAndGet(this, input)
        }
    }

    @Test
    fun `test integer field updater`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val result = taskFactory.create(HasIntegerField::class.java)
            .apply(4567)
        assertThat(result).isEqualTo(4567)
    }

    class HasIntegerField : Function<Int, Int> {
        @Suppress("unused")
        @Volatile
        private var integerField: Int = 0

        override fun apply(input: Int): Int {
            val updater = AtomicIntegerFieldUpdater.newUpdater(javaClass, "integerField")
            return updater.addAndGet(this, input)
        }
    }

    @Test
    fun `test reference field updater`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val result = taskFactory.create(HasReferenceField::class.java)
            .apply("Hello World!")
        assertThat(result).isEqualTo("[tag:Hello World!]")
    }

    class HasReferenceField : Function<String, String> {
        @Suppress("unused")
        @Volatile
        private var referenceField: String = "tag"

        override fun apply(input: String): String {
            val updater = AtomicReferenceFieldUpdater.newUpdater(javaClass, String::class.java,"referenceField")
            return updater.updateAndGet(this) {
                "[$it:$input]"
            }
        }
    }
}