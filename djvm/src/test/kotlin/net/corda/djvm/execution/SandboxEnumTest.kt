package net.corda.djvm.execution

import net.corda.djvm.SandboxType.KOTLIN
import net.corda.djvm.TestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.EnumMap
import java.util.EnumSet
import java.util.function.Function

class SandboxEnumTest : TestBase(KOTLIN) {
    @Test
    fun `test enum inside sandbox`() = sandbox {
        val taskFactory = classLoader.createTaskFactory()
        val result = classLoader.typedTaskFor<Int, Array<String>, TransformEnum>(taskFactory)
            .apply(0)
        assertThat(result).isEqualTo(arrayOf("ONE", "TWO", "THREE"))
    }

    @Test
    fun `return enum from sandbox`() = sandbox {
        val taskFactory = classLoader.createTaskFactory()
        val result = classLoader.typedTaskFor<String, ExampleEnum, FetchEnum>(taskFactory)
            .apply("THREE")
        assertThat(result).isEqualTo(ExampleEnum.THREE)
    }

    @Test
    fun `test we can identify class as Enum`() = sandbox {
        val taskFactory = classLoader.createTaskFactory()
        val result = classLoader.typedTaskFor<ExampleEnum, Boolean, AssertEnum>(taskFactory)
            .apply(ExampleEnum.THREE)
        assertThat(result).isTrue()
    }

    @Test
    fun `test we can create EnumMap`() = sandbox {
        val taskFactory = classLoader.createTaskFactory()
        val result = classLoader.typedTaskFor<ExampleEnum, Int, UseEnumMap>(taskFactory)
            .apply(ExampleEnum.TWO)
        assertThat(result).isEqualTo(1)
    }

    @Test
    fun `test we can create EnumSet`() = sandbox {
        val taskFactory = classLoader.createTaskFactory()
        val result = classLoader.typedTaskFor<ExampleEnum, Boolean, UseEnumSet>(taskFactory)
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
