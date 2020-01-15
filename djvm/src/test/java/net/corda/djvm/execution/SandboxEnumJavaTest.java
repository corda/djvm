package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.TypedTaskFactory;
import net.corda.djvm.WithJava;
import org.junit.jupiter.api.Test;

import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

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
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String[] result = WithJava.run(taskFactory, TransformEnum.class, 0);
                assertThat(result)
                    .isEqualTo(new String[]{"ONE", "TWO", "THREE"});
            } catch(Exception e) {
                fail(e);
            }
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
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                ExampleEnum result = WithJava.run(taskFactory, FetchEnum.class, "THREE");
                assertThat(result)
                     .isEqualTo(ExampleEnum.THREE);
            } catch(Exception e) {
                fail(e);
            }
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
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Boolean result = WithJava.run(taskFactory, AssertEnum.class, ExampleEnum.THREE);
                assertThat(result).isTrue();
            } catch(Exception e) {
                fail(e);
            }
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
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Integer result = WithJava.run(taskFactory, UseEnumMap.class, ExampleEnum.TWO);
                assertThat(result).isEqualTo(1);
            } catch(Exception e) {
                fail(e);
            }
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
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Boolean result = WithJava.run(taskFactory, UseEnumSet.class, ExampleEnum.ONE);
                assertThat(result).isTrue();
            } catch(Exception e) {
                fail(e);
            }
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
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                ExampleEnum result = WithJava.run(taskFactory, ConstantEnum.class, null);
                assertThat(result).isEqualTo(ExampleEnum.ONE);
            } catch(Exception e) {
                fail(e);
            }
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
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                ExampleEnum result = WithJava.run(taskFactory, StaticConstantEnum.class, null);
                assertThat(result).isEqualTo(ExampleEnum.TWO);
            } catch(Exception e) {
                fail(e);
            }
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
