package net.corda.djvm.execution

import net.corda.djvm.SandboxType.KOTLIN
import net.corda.djvm.TestBase
import net.corda.djvm.rewiring.createRawPredicateFactory
import net.corda.djvm.rewiring.createSandboxPredicate
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.function.Predicate

class SandboxPredicateTest : TestBase(KOTLIN) {
    @ParameterizedTest
    @EnumSource(ExampleEnum::class)
    fun testPredicate(example: ExampleEnum) = sandbox {
        val sandboxPredicate = classLoader.createSandboxPredicate()
        val predicateFactory = classLoader.createRawPredicateFactory().compose(sandboxPredicate)
        val isSandboxEnum = predicateFactory.apply(CheckEnum::class.java)
        val sandboxEnum = classLoader.createBasicInput().apply(example)
                ?: fail("sandboxed enum should not be null")

        assertTrue(isSandboxEnum.test(sandboxEnum::class.java))
        assertFalse(isSandboxEnum.test(example::class.java))
    }

    @Test
    fun testEmptyEnum() = sandbox {
        val sandboxPredicate = classLoader.createSandboxPredicate()
        val predicateFactory = classLoader.createRawPredicateFactory().compose(sandboxPredicate)
        val isSandboxEnum = predicateFactory.apply(CheckEnum::class.java)
        val sandboxEnumClass = classLoader.toSandboxClass(EmptyEnum::class.java)
        assertTrue(isSandboxEnum.test(sandboxEnumClass))
        assertFalse(isSandboxEnum.test(EmptyEnum::class.java))
    }

    class CheckEnum : Predicate<Class<*>> {
        override fun test(clazz: Class<*>): Boolean {
            return clazz.isEnum
        }
    }
}