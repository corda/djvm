package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.WithJava;
import org.junit.jupiter.api.Test;

import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

class SandboxEnumJavaTest extends TestBase {
    SandboxEnumJavaTest() {
        super(JAVA);
    }

    @Test
    void testEnumInsideSandbox() {
        parentedSandbox(ctx -> {
            SandboxExecutor<Integer, String[]> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            ExecutionSummaryWithResult<String[]> output = WithJava.run(executor, TransformEnum.class, 0);
            assertThat(output.getResult())
                    .isEqualTo(new String[]{ "ONE", "TWO", "THREE" });
            return null;
        });
    }

    public static class TransformEnum implements Function<Integer, String[]> {
        @Override
        public String[] apply(Integer input) {
            return Stream.of(ExampleEnum.values()).map(ExampleEnum::name).toArray(String[]::new);
        }
    }

    @Test
    void testReturnEnumFromSandbox() {
        parentedSandbox(ctx -> {
            SandboxExecutor<String, ExampleEnum> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            ExecutionSummaryWithResult<ExampleEnum> output = WithJava.run(executor, FetchEnum.class, "THREE");
            assertThat(output.getResult())
                    .isEqualTo(ExampleEnum.THREE);
            return null;
        });
    }

    public static class FetchEnum implements Function<String, ExampleEnum> {
        public ExampleEnum apply(String input) {
            return ExampleEnum.valueOf(input);
        }
    }

    @Test
    void testWeCanIdentifyClassAsEnum() {
        parentedSandbox(ctx -> {
            SandboxExecutor<ExampleEnum, Boolean> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            ExecutionSummaryWithResult<Boolean> output = WithJava.run(executor, AssertEnum.class, ExampleEnum.THREE);
            assertThat(output.getResult()).isTrue();
            return null;
        });
    }

    public static class AssertEnum implements Function<ExampleEnum, Boolean> {
        @Override
        public Boolean apply(ExampleEnum input) {
            return input.getClass().isEnum();
        }
    }

    @Test
    void testWeCanCreateEnumMap() {
        parentedSandbox(ctx -> {
            SandboxExecutor<ExampleEnum, Integer> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            ExecutionSummaryWithResult<Integer> output = WithJava.run(executor, UseEnumMap.class, ExampleEnum.TWO);
            assertThat(output.getResult()).isEqualTo(1);
            return null;
        });
    }

    public static class UseEnumMap implements Function<ExampleEnum, Integer> {
        @Override
        public Integer apply(ExampleEnum input) {
            Map<ExampleEnum, String> map = new EnumMap<>(ExampleEnum.class);
            map.put(input, input.name());
            return map.size();
        }
    }

    @Test
    void testWeCanCreateEnumSet() {
        parentedSandbox(ctx -> {
            SandboxExecutor<ExampleEnum, Boolean> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            ExecutionSummaryWithResult<Boolean> output = WithJava.run(executor, UseEnumSet.class, ExampleEnum.ONE);
            assertThat(output.getResult()).isTrue();
            return null;
        });
    }

    public static class UseEnumSet implements Function<ExampleEnum, Boolean> {
        @Override
        public Boolean apply(ExampleEnum input) {
            return EnumSet.allOf(ExampleEnum.class).contains(input);
        }
    }

    @Test
    void testWeCanReadConstantEnum() {
        parentedSandbox(ctx -> {
            SandboxExecutor<Object, ExampleEnum> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            ExecutionSummaryWithResult<ExampleEnum> output = WithJava.run(executor, ConstantEnum.class, null);
            assertThat(output.getResult()).isEqualTo(ExampleEnum.ONE);
            return null;
        });
    }

    public static class ConstantEnum implements Function<Object, ExampleEnum> {
        private final ExampleEnum value = ExampleEnum.ONE;

        @Override
        public ExampleEnum apply(Object input) {
            return value;
        }
    }

    @Test
    void testWeCanReadStaticConstantEnum() {
        parentedSandbox(ctx -> {
            SandboxExecutor<Object, ExampleEnum> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            ExecutionSummaryWithResult<ExampleEnum> output = WithJava.run(executor, StaticConstantEnum.class, null);
            assertThat(output.getResult()).isEqualTo(ExampleEnum.TWO);
            return null;
        });
    }

    public static class StaticConstantEnum implements Function<Object, ExampleEnum> {
        private static final ExampleEnum VALUE = ExampleEnum.TWO;

        @Override
        public ExampleEnum apply(Object input) {
            return VALUE;
        }
    }
}
