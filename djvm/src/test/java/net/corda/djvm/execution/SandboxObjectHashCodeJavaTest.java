package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.WithJava;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class SandboxObjectHashCodeJavaTest extends TestBase {
    SandboxObjectHashCodeJavaTest() {
        super(JAVA);
    }

    @Test
    void testHashForArray() {
        sandbox(ctx -> {
            SandboxExecutor<Object, Integer> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            ExecutionSummaryWithResult<Integer> output = WithJava.run(executor, ArrayHashCode.class, null);
            assertThat(output.getResult()).isEqualTo(0xfed_c0de + 1);
            return null;
        });
    }

    @Test
    void testHashForObjectInArray() {
        sandbox(ctx -> {
            SandboxExecutor<Object, Integer> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            ExecutionSummaryWithResult<Integer> output = WithJava.run(executor, ObjectInArrayHashCode.class, null);
            assertThat(output.getResult()).isEqualTo(0xfed_c0de + 1);
            return null;
        });
    }

    @Test
    void testHashForNullObject() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new HashCode().apply(null));

        sandbox(ctx -> {
            SandboxExecutor<Object, Integer> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> WithJava.run(executor, HashCode.class, null));
            return null;
        });
    }

    @Test
    void testHashForWrappedInteger() {
        sandbox(ctx -> {
            SandboxExecutor<Object, Integer> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            ExecutionSummaryWithResult<Integer> output = WithJava.run(executor, HashCode.class, 1234);
            assertThat(output.getResult()).isEqualTo(Integer.hashCode(1234));
            return null;
        });
    }

    @Test
    void testHashForWrappedString() {
        sandbox(ctx -> {
            SandboxExecutor<Object, Integer> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            ExecutionSummaryWithResult<Integer> output = WithJava.run(executor, HashCode.class, "Burble");
            assertThat(output.getResult()).isEqualTo("Burble".hashCode());
            return null;
        });
    }

    public static class ObjectInArrayHashCode implements Function<Object, Integer> {
        @Override
        public Integer apply(Object obj) {
            Object[] arr = new Object[1];
            arr[0] = new Object();
            return arr[0].hashCode();
        }
    }

    public static class ArrayHashCode implements Function<Object, Integer> {
        @SuppressWarnings("all")
        @Override
        public Integer apply(Object obj) {
            return new Object[0].hashCode();
        }
    }

    public static class HashCode implements Function<Object, Integer> {
        @Override
        public Integer apply(@Nullable Object obj) {
            return obj.hashCode();
        }
    }
}
