package net.corda.djvm.execution;

import net.corda.djvm.TaskExecutor;
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

    private Object run(TaskExecutor executor, Class<?> task, Object data) throws Exception {
        Object toStringTask = executor.toSandboxClass(task).newInstance();
        return executor.apply(toStringTask, data);
    }

    @Test
    void testFetchingPackage() {
        parentedSandbox(ctx -> {
            try {
                TaskExecutor executor = new TaskExecutor(ctx.getClassLoader());
                assertNull(run(executor, FetchPackage.class, "java.lang"));
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
        parentedSandbox(ctx -> {
            try {
                TaskExecutor executor = new TaskExecutor(ctx.getClassLoader());
                assertThat((String[]) run(executor, FetchAllPackages.class, null)).isEmpty();
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
