package com.example.testing;

import net.corda.djvm.TypedTaskFactory;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class JavaResetRuntimeContextTest extends TestBase {
    @Test
    void testHashCodesSurviveReset() {
        create(options -> {}, context -> {
            sandbox(context, ctx -> {
                try {
                    TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                    Function<? super Object, Integer> getStaticHashCode = taskFactory.create(GetStaticHashCode.class);
                    Function<? super Object, Integer> getHashCode = taskFactory.create(GetHashCode.class);
                    ctx.ready();

                    final int staticHashCode = getStaticHashCode.apply(null);
                    final int hashCode0 = getHashCode.apply("<A>");
                    final int hashCode1 = getHashCode.apply("<B>");
                    assertThat(asList(staticHashCode, hashCode0, hashCode1))
                        .containsExactly(0xfed_c0de - 1, 0xfed_c0de + 1, 0xfed_c0de + 2);
                } catch (Exception e) {
                    fail(e);
                }
            });

            sandbox(context, ctx -> {
                try {
                    TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                    Function<? super Object, Integer> getStaticHashCode = taskFactory.create(GetStaticHashCode.class);
                    Function<? super Object, Integer> getHashCode = taskFactory.create(GetHashCode.class);
                    ctx.ready();

                    final int staticHashCode = getStaticHashCode.apply(null);
                    final int hashCode0 = getHashCode.apply("<X>");
                    final int hashCode1 = getHashCode.apply("<Y>");
                    assertThat(asList(staticHashCode, hashCode0, hashCode1))
                        .containsExactly(0xfed_c0de - 1, 0xfed_c0de + 1, 0xfed_c0de + 2);
                } catch (Exception e) {
                    fail(e);
                }
            });
        });
    }
}
