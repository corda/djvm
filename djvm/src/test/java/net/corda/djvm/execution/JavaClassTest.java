package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.TypedTaskFactory;
import net.corda.djvm.WithJava;
import net.corda.djvm.rewiring.SandboxClassLoader;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class JavaClassTest extends TestBase {
    JavaClassTest() {
        super(JAVA);
    }

    @Test
    void testGetClassNames() {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                DJVM djvm = new DJVM(classLoader);

                Function<Class<? extends Function<?, ?>>, ? extends Function<? super Object, ?>> taskFactory
                    = classLoader.createRawTaskFactory().compose(classLoader.createSandboxFunction());
                Object[] results = (Object[]) taskFactory.apply(GetClassNames.class).apply(null);
                assertThat(results).containsExactly(
                    djvm.stringOf("sandbox.net.corda.djvm.execution.GetClassNames"),
                    djvm.stringOf("GetClassNames"),
                    djvm.stringOf("sandbox.net.corda.djvm.execution.GetClassNames"),
                    djvm.stringOf("sandbox.net.corda.djvm.execution.GetClassNames"),
                    djvm.stringOf("public class sandbox.net.corda.djvm.execution.GetClassNames"),
                    djvm.stringOf("class sandbox.net.corda.djvm.execution.GetClassNames")
                );
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testGetClassNameByFunctionReference() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String[] results = WithJava.run(taskFactory, GetClassNameReference.class, null);
                assertThat(results).containsExactly(
                    "sandbox.java.lang.String",
                    "java.lang.Object",
                    "sandbox.java.util.ArrayList"
                );
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class GetClassNameReference implements Function<String, String[]> {
        @Override
        public String[] apply(String unused) {
            Class<?>[] classes = new Class<?>[]{ String.class, Object.class, ArrayList.class };
            return Arrays.stream(classes)
                .map(Class::getName)
                .toArray(String[]::new);
        }
    }
}