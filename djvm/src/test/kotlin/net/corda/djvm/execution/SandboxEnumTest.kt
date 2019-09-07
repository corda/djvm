package net.corda.djvm.execution

import net.corda.djvm.SandboxType.KOTLIN
import net.corda.djvm.TestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*
import java.util.function.Function

class SandboxEnumTest : TestBase(KOTLIN) {
    @Test
    fun `test enum inside sandbox`() = parentedSandbox {
        val executor = classLoader.createExecutor()
        val result = classLoader.typedTaskFor<Int, Array<String>, TransformEnum>(executor)
            .apply(0)
        assertThat(result).isEqualTo(arrayOf("ONE", "TWO", "THREE"))
    }

    @Test
    fun `return enum from sandbox`() = parentedSandbox {
        val executor = classLoader.createExecutor()
        val result = classLoader.typedTaskFor<String, ExampleEnum, FetchEnum>(executor)
            .apply("THREE")
        assertThat(result).isEqualTo(ExampleEnum.THREE)
    }

    @Test
    fun `test we can identify class as Enum`() = parentedSandbox {
        val executor = classLoader.createExecutor()
        val result = classLoader.typedTaskFor<ExampleEnum, Boolean, AssertEnum>(executor)
            .apply(ExampleEnum.THREE)
        assertThat(result).isTrue()
    }

    @Test
    fun `test we can create EnumMap`() = parentedSandbox {
        val executor = classLoader.createExecutor()
        val result = classLoader.typedTaskFor<ExampleEnum, Int, UseEnumMap>(executor)
            .apply(ExampleEnum.TWO)
        assertThat(result).isEqualTo(1)
    }

    @Test
    fun `test we can create EnumSet`() = parentedSandbox {
        val executor = classLoader.createExecutor()
        val result = classLoader.typedTaskFor<ExampleEnum, Boolean, UseEnumSet>(executor)
            .apply(ExampleEnum.ONE)
        assertThat(result).isTrue()
    }
}


class AssertEnum : Function<ExampleEnum, Boolean> {
    override fun apply(input: ExampleEnum): Boolean {
        return input::class.java.isEnum
    }
}

class TransformEnum : Function<Int, Array<String>> {
    override fun apply(input: Int): Array<String> {
        return ExampleEnum.values().map(ExampleEnum::name).toTypedArray()
    }
}

class FetchEnum : Function<String, ExampleEnum> {
    override fun apply(input: String): ExampleEnum {
        return ExampleEnum.valueOf(input)
    }
}

class UseEnumMap : Function<ExampleEnum, Int> {
    override fun apply(input: ExampleEnum): Int {
        val map = EnumMap<ExampleEnum, String>(ExampleEnum::class.java)
        map[input] = input.name
        return map.size
    }
}

class UseEnumSet : Function<ExampleEnum, Boolean> {
    override fun apply(input: ExampleEnum): Boolean {
        return EnumSet.allOf(ExampleEnum::class.java).contains(input)
    }
}
