package net.corda.djvm.execution;

import net.corda.djvm.AnnotationUtils;
import net.corda.djvm.Cowboy;
import net.corda.djvm.JavaAnnotation;
import net.corda.djvm.JavaLabel;
import net.corda.djvm.TestBase;
import net.corda.djvm.TypedTaskFactory;
import net.corda.djvm.WithJava;
import net.corda.djvm.rewiring.SandboxClassLoader;
import net.corda.djvm.rules.RuleViolationError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import sandbox.java.lang.DJVMClass;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;

import static net.corda.djvm.SandboxType.JAVA;
import static net.corda.djvm.code.Types.isClassMethodThunk;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class JavaClassTest extends TestBase {
    JavaClassTest() {
        super(JAVA);
    }

    static class ClassMethodThunkSource implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            Stream<String> thunkNames = Arrays.stream(DJVMClass.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .map(Method::getName);
            return Stream.concat(thunkNames, Stream.of("enumConstantDirectory"))
                .map(Arguments::of);
        }
    }

    @ParameterizedTest(name = "declared as thunk: {index} => DJVMClass.{0}")
    @ArgumentsSource(ClassMethodThunkSource.class)
    void validateClassMethodThunks(String thunkName) {
        assertTrue(isClassMethodThunk(thunkName));
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
}