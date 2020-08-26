package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.TypedTaskFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static java.util.Arrays.asList;
import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class ResetObjectHashCodesTest extends TestBase {
    ResetObjectHashCodesTest() {
        super(JAVA);
    }

    @Test
    void testInitialHashCodesDecrease() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Function<? super Object, Integer> getHashCode = taskFactory.create(GetHashCode.class);

                final int hashCode0 = getHashCode.apply("<X>");
                final int hashCode1 = getHashCode.apply("<Y>");
                assertThat(asList(hashCode0, hashCode1)).containsExactly(0xfed_c0de - 1, 0xfed_c0de - 2);

                reset(ctx);

                final int hashCode2 = getHashCode.apply("<X>");
                final int hashCode3 = getHashCode.apply("<Y>");
                assertThat(asList(hashCode2, hashCode3)).containsExactly(0xfed_c0de - 1, 0xfed_c0de - 2);
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testHashCodesIncreaseOnceReady() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Function<? super Object, Integer> getHashCode = taskFactory.create(GetHashCode.class);
                ctx.ready();

                final int hashCode0 = getHashCode.apply("<A>");
                final int hashCode1 = getHashCode.apply("<B>");
                assertThat(asList(hashCode0, hashCode1)).containsExactly(0xfed_c0de + 1, 0xfed_c0de + 2);

                reset(ctx);
                ctx.ready();

                final int hashCode2 = getHashCode.apply("<A>");
                final int hashCode3 = getHashCode.apply("<B>");
                assertThat(asList(hashCode2, hashCode3)).containsExactly(0xfed_c0de + 1, 0xfed_c0de + 2);
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class GetHashCode implements Function<Object, Integer> {
        @Override
        public Integer apply(@NotNull Object obj) {
            return System.identityHashCode(obj);
        }
    }

    @Test
    void testInitialHashCodesSurviveReset() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Function<? super Object, Integer> getStaticHashCode = taskFactory.create(GetStaticHashCode.class);
                Function<? super Object, Integer> getHashCode = taskFactory.create(GetHashCode.class);
                ctx.ready();

                final int staticHashCode0 = getStaticHashCode.apply(null);
                final int hashCode0 = getHashCode.apply("<A>");
                final int hashCode1 = getHashCode.apply("<B>");
                assertThat(asList(staticHashCode0, hashCode0, hashCode1))
                    .containsExactly(0xfed_c0de - 1, 0xfed_c0de + 1, 0xfed_c0de + 2);

                reset(ctx);
                ctx.ready();

                final int staticHashCode1 = getStaticHashCode.apply(null);
                final int hashCode2 = getHashCode.apply("<A>");
                final int hashCode3 = getHashCode.apply("<B>");
                assertThat(asList(staticHashCode1, hashCode2, hashCode3))
                    .containsExactly(0xfed_c0de - 1, 0xfed_c0de + 1, 0xfed_c0de + 2);
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class GetStaticHashCode implements Function<Object, Integer> {
        private static final int HASH_CODE = GetStaticHashCode.class.hashCode();

        @Override
        public Integer apply(Object obj) {
            return HASH_CODE;
        }
    }
}
