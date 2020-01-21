package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.TypedTaskFactory;
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
                Function<String, String> fetchPackage = taskFactory.create(FetchPackage.class);
                assertNull(fetchPackage.apply("java.lang"));
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
                Function<Object, String[]> fetchAllPackages = taskFactory.create(FetchAllPackages.class);
                assertThat(fetchAllPackages.apply(null)).isEmpty();
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class FetchAllPackages implements Function<Object, String[]> {
        @Override
        public String[] apply(Object input) {
            return Arrays.stream(Package.getPackages()).map(Package::getName).toArray(String[]::new);
        }
    }
}
