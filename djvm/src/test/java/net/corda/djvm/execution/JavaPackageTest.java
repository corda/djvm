package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.TypedTaskFactory;
import net.corda.djvm.WithJava;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

class JavaPackageTest extends TestBase {
    JavaPackageTest() {
        super(JAVA);
    }

    @Test
    void testFetchingPackage() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Object result = WithJava.run(taskFactory, FetchPackage.class, "java.lang");
                assertNull(result);
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class FetchPackage implements Function<String, String> {
        @Override
        public String apply(String packageName) {
            Package pkg = Package.getPackage(packageName);
            return (pkg == null) ? null : pkg.getName();
        }
    }

    @Test
    void testFetchingAllPackage() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String[] result = WithJava.run(taskFactory, FetchAllPackages.class, null);
                assertThat(result).isEmpty();
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class FetchAllPackages implements Function<Object, String[]> {
        @Override
        public String[] apply(Object input) {
            return Arrays.stream(Package.getPackages())
                .map(Package::getName)
                .toArray(String[]::new);
        }
    }
}
