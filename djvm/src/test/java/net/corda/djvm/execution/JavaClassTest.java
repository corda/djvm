package net.corda.djvm.execution;

import com.example.testing.HappyObject;
import net.corda.djvm.AnnotationUtils;
import net.corda.djvm.BouncyCastle;
import net.corda.djvm.Cowboy;
import net.corda.djvm.ExceptionalFunction;
import net.corda.djvm.JavaAnnotation;
import net.corda.djvm.JavaLabel;
import net.corda.djvm.TestBase;
import net.corda.djvm.TypedTaskFactory;
import net.corda.djvm.WithJava;
import net.corda.djvm.api.RuleViolationError;
import net.corda.djvm.rewiring.SandboxClassLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.InputStream;
import java.net.URL;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@ExtendWith(BouncyCastle.class)
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

    @Test
    void testGetResourceByMethodReference() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Object result = WithJava.run(taskFactory,
                    GetResourceByReference.class, "/META-INF/MANIFEST.MF"
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
            Function<String, URL> getResource = getClass()::getResource;
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
                    GetResourceStreamByReference.class, "/META-INF/MANIFEST.MF"
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
            Function<String, InputStream> getResource = getClass()::getResourceAsStream;
            return getResource.apply(input);
        }
    }

    @Test
    void testShortClassForNameByMethodReference() {
        sandbox(ctx -> {
            try {
                String className = HappyObject.class.getName();
                SandboxClassLoader classLoader = ctx.getClassLoader();
                TypedTaskFactory taskFactory = classLoader.createTypedTaskFactory();
                Class<?> sandboxClass = WithJava.run(taskFactory, ShortClassForNameByReference.class, className);
                assertEquals("sandbox." + className, sandboxClass.getName());
                assertSame(classLoader, sandboxClass.getClassLoader());

                RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> WithJava.run(taskFactory, ShortClassForNameByReference.class, ExampleEnum.class.getName()));
                assertThat(ex)
                    .hasCauseExactlyInstanceOf(ClassNotFoundException.class)
                    .hasMessage(ExampleEnum.class.getName());
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class ShortClassForNameByReference implements Function<String, Class<?>> {
        @Override
        public Class<?> apply(String className) {
            ExceptionalFunction<String, Class<?>> loader = Class::forName;
            try {
                return loader.apply(className);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    @Test
    void testFullClassForNameByMethodReference() {
        sandbox(ctx -> {
            try {
                String className = HappyObject.class.getName();
                SandboxClassLoader classLoader = ctx.getClassLoader();
                TypedTaskFactory taskFactory = classLoader.createTypedTaskFactory();
                Class<?> sandboxClass = WithJava.run(taskFactory, FullClassForNameByReference.class, className);
                assertEquals("sandbox." + className, sandboxClass.getName());
                assertSame(classLoader, sandboxClass.getClassLoader());

                RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> WithJava.run(taskFactory, FullClassForNameByReference.class, ExampleEnum.class.getName()));
                assertThat(ex)
                    .hasCauseExactlyInstanceOf(ClassNotFoundException.class)
                    .hasMessage(ExampleEnum.class.getName());
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class FullClassForNameByReference implements Function<String, Class<?>> {
        interface ClassLoading {
            Class<?> load(String className, boolean initialize, ClassLoader classLoader)
                    throws ClassNotFoundException;
        }

        @Override
        public Class<?> apply(String className) {
            ClassLoading loader = Class::forName;
            try {
                return loader.load(className, false, ClassLoader.getSystemClassLoader());
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    @Test
    void testClassMethodReferenceWithPrimitiveReturnType() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Boolean[] results = WithJava.run(taskFactory, SpotTheEnum.class, null);
                assertThat(results).containsExactly(true, false, false, true);
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class SpotTheEnum implements Function<String, Boolean[]> {
        @SuppressWarnings("UnnecessaryBoxing")
        @Override
        public Boolean[] apply(String s) {
            Class<?>[] candidates = new Class<?>[]{
                ExampleEnum.class,
                SpotTheEnum.class,
                Object.class,
                Cowboy.class
            };
            return Arrays.stream(candidates)
                // We currently NEED to box this primitive type manually.
                // The DJVM cannot inject:
                //     "boolean -> sandbox.java.lang.Boolean"
                // into this byte-code yet.
                .map(c -> Boolean.valueOf(c.isEnum()))
                .toArray(Boolean[]::new);
        }
    }

    @Test
    void testEnumConstantsByMethodReference() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String[] results = WithJava.run(taskFactory, GetEnumConstants.class, null);
                assertThat(results).containsExactly("GOOD", "BAD", "UGLY");
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class GetEnumConstants implements Function<String, String[]> {
        @Override
        public String[] apply(String s) {
            Class<?>[] candidates = new Class<?>[] { Cowboy.class };
            return Arrays.stream(candidates)
                .map(Class::getEnumConstants)
                .flatMap(Arrays::stream)
                .map(Object::toString)
                .toArray(String[]::new);
        }
    }

    @Test
    void testGetAnnotationsByMethodReference() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String[] results = WithJava.run(taskFactory, BulkFetchAnnotations.class, null);
                assertThat(results).containsExactly(
                    "@sandbox.net.corda.djvm.JavaAnnotation(value=Bulk Fetch)",
                    "@sandbox.net.corda.djvm.JavaLabel(name=Happy)"
                );
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @JavaAnnotation("Bulk Fetch")
    @JavaLabel(name = "Happy")
    public static class BulkFetchAnnotations implements Function<String, String[]> {
        @Override
        public String[] apply(String s) {
            Class<?>[] candidates = new Class<?>[]{
                BulkFetchAnnotations.class
            };
            return Arrays.stream(candidates)
                .map(Class::getAnnotations)
                .flatMap(AnnotationUtils::toStrings)
                .toArray(String[]::new);
        }
    }

    @Test
    void testGetGenericInterfacesIsForbidden() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                RuleViolationError ex = assertThrows(RuleViolationError.class,
                    () -> WithJava.run(taskFactory, GetGenericInterfaces.class, null));
                assertThat(ex)
                    .hasMessage("Disallowed reference to API; java.lang.Class.getGenericInterfaces()")
                    .hasNoCause();
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class GetGenericInterfaces implements Function<String, Object> {
        @Override
        public Object apply(String s) {
            return getClass().getGenericInterfaces();
        }
    }

    @Test
    void testGetAnnotatedInterfacesIsForbidden() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                RuleViolationError ex = assertThrows(RuleViolationError.class,
                    () -> WithJava.run(taskFactory, GetAnnotatedInterfaces.class, null));
                assertThat(ex)
                    .hasMessage("Disallowed reference to API; java.lang.Class.getAnnotatedInterfaces()")
                    .hasNoCause();
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class GetAnnotatedInterfaces implements Function<String, Object> {
        @Override
        public Object apply(String unused) {
            return getClass().getAnnotatedInterfaces();
        }
    }

    @Test
    void testGetGenericSuperclassIsForbidden() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                RuleViolationError ex = assertThrows(RuleViolationError.class,
                    () -> WithJava.run(taskFactory, GetGenericSuperclass.class, null));
                assertThat(ex)
                    .hasMessage("Disallowed reference to API; java.lang.Class.getGenericSuperclass()")
                    .hasNoCause();
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class GetGenericSuperclass implements Function<String, Object> {
        @Override
        public Object apply(String unused) {
            return getClass().getGenericSuperclass();
        }
    }

    @Test
    void testGetAnnotatedSuperclassIsForbidden() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                RuleViolationError ex = assertThrows(RuleViolationError.class,
                    () -> WithJava.run(taskFactory, GetAnnotatedSuperclass.class, null));
                assertThat(ex)
                    .hasMessage("Disallowed reference to API; java.lang.Class.getAnnotatedSuperclass()")
                    .hasNoCause();
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class GetAnnotatedSuperclass implements Function<String, Object> {
        @Override
        public Object apply(String unused) {
            return getClass().getAnnotatedSuperclass();
        }
    }

    @Test
    void testGetTypeParametersIsForbidden() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                RuleViolationError ex = assertThrows(RuleViolationError.class,
                    () -> WithJava.run(taskFactory, GetTypeParameters.class, null));
                assertThat(ex)
                    .hasMessage("Disallowed reference to API; java.lang.Class.getTypeParameters()")
                    .hasNoCause();
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class GetTypeParameters implements Function<String, Object> {
        @Override
        public Object apply(String unused) {
            return getClass().getTypeParameters();
        }
    }

    @Test
    void testGetSignersIsAlwaysNull() {
        /*
         * I am using security providers as examples of classes
         * which have been signed.
         */
        Optional<? extends Class<?>> signedClass = Arrays.stream(Security.getProviders())
            .map(Object::getClass)
            .filter(c -> c.getSigners() != null)
            .findFirst();
        assumeTrue(signedClass.isPresent(), "No example of a signed class found.");

        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                Object result = classLoader.createRawTaskFactory()
                    .compose(classLoader.createSandboxFunction())
                    .apply(GetSigners.class)
                    .apply(signedClass.get());
                assertNull(result);
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class GetSigners implements Function<Class<?>, Object[]> {
        @Override
        public Object[] apply(Class<?> signed) {
            return signed.getSigners();
        }
    }
}