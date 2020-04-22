package net.corda.djvm.execution;

import com.example.testing.HappyObject;
import net.corda.djvm.ExceptionalFunction;
import net.corda.djvm.TestBase;
import net.corda.djvm.TypedTaskFactory;
import net.corda.djvm.WithJava;
import net.corda.djvm.rewiring.SandboxClassLoader;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
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
        @Override
        public Class<?> apply(String className) {
            ExceptionalFunction<String, Class<?>> load = ClassLoader.getSystemClassLoader()::loadClass;
            try {
                return load.apply(className);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    @Test
    void testGetParentByMethodReference() {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                Function<? super Object, ?> getParent = classLoader.createRawTaskFactory()
                    .compose(classLoader.createSandboxFunction())
                    .apply(GetParentByReference.class);
                Object result = getParent.apply(null);
                assertThat(result).isNull();
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class GetParentByReference implements Function<String, ClassLoader> {
        @Override
        public ClassLoader apply(String unused) {
            Supplier<ClassLoader> getParent = ClassLoader.getSystemClassLoader()::getParent;
            return getParent.get();
        }
    }

    @Test
    void testGetResourcesByMethodReference() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Object result = WithJava.run(taskFactory,
                    GetResourcesByReference.class, "META-INF/MANIFEST.MF"
                );
                assertThat(result).isInstanceOf(String[].class);
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class GetResourcesByReference implements Function<String, String[]> {
        @Override
        public String[] apply(String input) {
            ExceptionalFunction<String, Enumeration<URL>> getResources = ClassLoader.getSystemClassLoader()::getResources;
            try {
                return Collections.list(getResources.apply(input)).stream()
                    .map(Object::toString)
                    .toArray(String[]::new);
            } catch(Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    @Test
    void testGetResourceByMethodReference() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Object result = WithJava.run(taskFactory,
                    GetResourceByReference.class, "META-INF/MANIFEST.MF"
                );
                assertThat(result).isNull();
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class GetResourceByReference implements Function<String, String> {
        @Override
        public String apply(String input) {
            Function<String, URL> getResource = ClassLoader.getSystemClassLoader()::getResource;
            URL resource = getResource.apply(input);
            return (resource == null) ? null : resource.toString();
        }
    }

    @Test
    void testGetResourceStreamByMethodReference() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Object result = WithJava.run(taskFactory,
                    GetResourceStreamByReference.class, "META-INF/MANIFEST.MF"
                );
                assertThat(result).isNull();
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class GetResourceStreamByReference implements Function<String, InputStream> {
        @Override
        public InputStream apply(String input) {
            Function<String, InputStream> getResource = ClassLoader.getSystemClassLoader()::getResourceAsStream;
            return getResource.apply(input);
        }
    }

    @Test
    void testGetSystemResourcesByMethodReference() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Object result = WithJava.run(taskFactory,
                    GetSystemResourcesByReference.class, "META-INF/MANIFEST.MF"
                );
                assertThat(result).isInstanceOf(String[].class);
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class GetSystemResourcesByReference implements Function<String, String[]> {
        @Override
        public String[] apply(String input) {
            ExceptionalFunction<String, Enumeration<URL>> getSystemResources = ClassLoader::getSystemResources;
            try {
                return Collections.list(getSystemResources.apply(input)).stream()
                    .map(Object::toString)
                    .toArray(String[]::new);
            } catch(Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    @Test
    void testGetSystemResourceByMethodReference() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Object result = WithJava.run(taskFactory,
                    GetSystemResourceByReference.class, "META-INF/MANIFEST.MF"
                );
                assertThat(result).isNull();
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class GetSystemResourceByReference implements Function<String, String> {
        @Override
        public String apply(String input) {
            Function<String, URL> getSystemResource = ClassLoader::getSystemResource;
            URL resource = getSystemResource.apply(input);
            return (resource == null) ? null : resource.toString();
        }
    }

    @Test
    void testGetSystemResourceStreamByMethodReference() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Object result = WithJava.run(taskFactory,
                    GetSystemResourceStreamByReference.class, "META-INF/MANIFEST.MF"
                );
                assertThat(result).isNull();
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class GetSystemResourceStreamByReference implements Function<String, InputStream> {
        @Override
        public InputStream apply(String input) {
            Function<String, InputStream> getSystemResource = ClassLoader::getSystemResourceAsStream;
            return getSystemResource.apply(input);
        }
    }
}