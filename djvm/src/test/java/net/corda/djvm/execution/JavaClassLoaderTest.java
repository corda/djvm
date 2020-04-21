package net.corda.djvm.execution;

import com.example.testing.HappyObject;
import net.corda.djvm.TestBase;
import net.corda.djvm.rewiring.SandboxClassLoader;
import org.junit.jupiter.api.Test;

import java.util.function.Function;
import java.util.function.Supplier;

import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class JavaClassLoaderTest extends TestBase {
    JavaClassLoaderTest() {
        super(JAVA);
    }

    @Test
    void testSystemClassLoaderByMethodReference() {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                Function<? super Object, ?> getSystemClassLoader = classLoader.createRawTaskFactory()
                    .compose(classLoader.createSandboxFunction())
                    .apply(GetSystemClassLoaderByReference.class);
                Object result = getSystemClassLoader.apply(null);
                assertThat(result).isSameAs(classLoader);
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class GetSystemClassLoaderByReference implements Function<String, ClassLoader> {
        @Override
        public ClassLoader apply(String unused) {
            Supplier<ClassLoader> cl = ClassLoader::getSystemClassLoader;
            return cl.get();
        }
    }

    @Test
    void testLoadClassByMethodReference() {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                DJVM djvm = new DJVM(classLoader);

                Function<? super Object, ?> getSystemClassLoader = classLoader.createRawTaskFactory()
                    .compose(classLoader.createSandboxFunction())
                    .apply(LoadClassByReference.class);
                Object className = djvm.stringOf(HappyObject.class.getName());
                Object result = getSystemClassLoader.apply(className);
                assertThat(result).isInstanceOf(Class.class);

                Class<?> sandboxClass = (Class<?>) result;
                assertThat(sandboxClass.getClassLoader()).isSameAs(classLoader);
                assertThat(sandboxClass.getName()).isEqualTo("sandbox.com.example.testing.HappyObject");
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class LoadClassByReference implements Function<String, Class<?>> {
        @FunctionalInterface
        interface ExceptionFunction<T, R> {
            R apply(T input) throws Exception;
        }

        @Override
        public Class<?> apply(String className) {
            ExceptionFunction<String, Class<?>> load = ClassLoader.getSystemClassLoader()::loadClass;
            try {
                return load.apply(className);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }
}