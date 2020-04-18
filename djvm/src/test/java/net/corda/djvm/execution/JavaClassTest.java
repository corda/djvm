package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.TypedTaskFactory;
import net.corda.djvm.WithJava;
import net.corda.djvm.rewiring.SandboxClassLoader;
import net.corda.djvm.rules.RuleViolationError;
import org.junit.jupiter.api.Test;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    public static class GetGenericInterfaces implements Function<String, String[]> {
        @Override
        public String[] apply(String s) {
            return Arrays.stream(GetGenericInterfaces.class.getGenericInterfaces())
                .map(Type::getTypeName)
                .toArray(String[]::new);
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

    public static class GetAnnotatedInterfaces implements Function<String, String[]> {
        @Override
        public String[] apply(String unused) {
            return Arrays.stream(GetAnnotatedInterfaces.class.getAnnotatedInterfaces())
                .map(AnnotatedType::getType)
                .map(Type::getTypeName)
                .toArray(String[]::new);
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

    public static class GetGenericSuperclass implements Function<String, String> {
        @Override
        public String apply(String unused) {
            Type superclass = GetGenericSuperclass.class.getGenericSuperclass();
            return (superclass == null) ? null : superclass.getTypeName();
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

    public static class GetAnnotatedSuperclass implements Function<String, String> {
        @Override
        public String apply(String unused) {
            AnnotatedType superclass = GetAnnotatedSuperclass.class.getAnnotatedSuperclass();
            return (superclass == null) ? null : superclass.getType().getTypeName();
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

    public static class GetTypeParameters implements Function<String, String[]> {
        @Override
        public String[] apply(String unused) {
            return Arrays.stream(GetTypeParameters.class.getTypeParameters())
                .map(TypeVariable::getTypeName)
                .toArray(String[]::new);
        }
    }
}