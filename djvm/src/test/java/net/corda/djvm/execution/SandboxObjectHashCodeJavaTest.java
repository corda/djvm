package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.TypedTaskFactory;
import net.corda.djvm.WithJava;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class SandboxObjectHashCodeJavaTest extends TestBase {
    SandboxObjectHashCodeJavaTest() {
        super(JAVA);
    }

    @Test
    void testHashForArray() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Integer result = WithJava.run(taskFactory, ArrayHashCode.class, null);
                assertThat(result).isEqualTo(0xfed_c0de + 1);
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testHashForObjectInArray() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Integer result = WithJava.run(taskFactory, ObjectInArrayHashCode.class, null);
                assertThat(result).isEqualTo(0xfed_c0de + 1);
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testHashForNullObject() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new HashCode().apply(null));

        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                assertThatExceptionOfType(NullPointerException.class)
                    .isThrownBy(() -> WithJava.run(taskFactory, HashCode.class, null));
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testHashForWrappedInteger() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Integer result = WithJava.run(taskFactory, HashCode.class, 1234);
                assertThat(result).isEqualTo(Integer.hashCode(1234));
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testHashForWrappedString() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Integer result = WithJava.run(taskFactory, HashCode.class, "Burble");
                assertThat(result).isEqualTo("Burble".hashCode());
            } catch(Exception e) {
                fail(e);
            }
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
