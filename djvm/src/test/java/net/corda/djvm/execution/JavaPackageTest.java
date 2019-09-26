package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
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
                Function<? super Object, ? extends Function<? super Object, ?>> taskFactory = ctx.getClassLoader().createTaskFactory();
                Function<String, String> fetchPackage = typedTaskFor(ctx.getClassLoader(), taskFactory, FetchPackage.class);
                assertNull(fetchPackage.apply("java.lang"));
            } catch (Exception e) {
                fail(e);
            }
            return null;
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
                Function<? super Object, ? extends Function<? super Object, ?>> taskFactory = ctx.getClassLoader().createTaskFactory();
                Function<Object, String[]> fetchAllPackages = typedTaskFor(ctx.getClassLoader(), taskFactory, FetchAllPackages.class);
                assertThat(fetchAllPackages.apply(null)).isEmpty();
            } catch (Exception e) {
                fail(e);
            }
            return null;
        });
    }

    public static class FetchAllPackages implements Function<Object, String[]> {
        @Override
        public String[] apply(Object input) {
            return Arrays.stream(Package.getPackages()).map(Package::getName).toArray(String[]::new);
        }
    }
}
