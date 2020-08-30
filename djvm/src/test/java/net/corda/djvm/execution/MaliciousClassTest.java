package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.TypedTaskFactory;
import net.corda.djvm.WithJava;
import net.corda.djvm.rewiring.SandboxClassLoadingException;
import net.corda.djvm.rules.RuleViolationError;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class MaliciousClassTest extends TestBase {
    MaliciousClassTest() {
        super(JAVA);
    }

    @Test
    void testImplementingToDJVMString() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Throwable ex = assertThrows(SandboxClassLoadingException.class, () -> WithJava.run(taskFactory, EvilToString.class, ""));
                assertThat(ex)
                    .hasMessageContaining("Class is not allowed to implement toDJVMString()");
            } catch(Exception e){
                fail(e);
            }
        });
    }

    public static class EvilToString implements Function<String, String> {
        @Override
        public String apply(String s) {
            return toString();
        }

        @SuppressWarnings("unused")
        public String toDJVMString() {
            throw new IllegalStateException("Victory is mine!");
        }
    }

    @Test
    void testImplementingFromDJVM() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Throwable ex = assertThrows(SandboxClassLoadingException.class, () -> WithJava.run(taskFactory, EvilFromDJVM.class, null));
                assertThat(ex)
                    .hasMessageContaining("Class is not allowed to implement fromDJVM()");
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class EvilFromDJVM implements Function<Object, Object> {
        @Override
        public Object apply(Object obj) {
            return this;
        }

        @SuppressWarnings("unused")
        protected Object fromDJVM() {
            throw new IllegalStateException("Victory is mine!");
        }
    }

    @Test
    void testPassingClassIntoSandboxIsForbidden() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Throwable ex = assertThrows(RuleViolationError.class, () -> WithJava.run(taskFactory, EvilClass.class, String.class));
                assertThat(ex)
                    .hasMessageContaining("Cannot sandbox class java.lang.String");
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class EvilClass implements Function<Class<?>, String> {
        @Override
        public String apply(Class<?> clazz) {
            return clazz.getName();
        }
    }

    @Test
    void testPassingForeignConstructorIntoSandboxIsForbidden() throws NoSuchMethodException {
        Constructor<?> constructor = getClass().getDeclaredConstructor();
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Throwable ex = assertThrows(RuleViolationError.class, () -> WithJava.run(taskFactory, EvilConstructor.class, constructor));
                assertThat(ex)
                    .hasMessageContaining("Cannot sandbox " + constructor);
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class EvilConstructor implements Function<Constructor<?>, String> {
        @Override
        public String apply(Constructor<?> constructor) {
            return constructor.getName();
        }
    }

    @Test
    void testPassingClassLoaderIntoSandboxIsForbidden() {
        ClassLoader classLoader = getClass().getClassLoader();
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Throwable ex = assertThrows(RuleViolationError.class, () -> WithJava.run(taskFactory, EvilClassLoader.class, classLoader));
                assertThat(ex)
                    .hasMessageContaining("Cannot sandbox a ClassLoader");
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class EvilClassLoader implements Function<ClassLoader, String> {
        @Override
        public String apply(ClassLoader classLoader) {
            return classLoader.toString();
        }
    }

    @Test
    void testCannotInvokeSandboxMethodsExplicitly() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Throwable ex = assertThrows(SandboxClassLoadingException.class,
                        () -> WithJava.run(taskFactory, SelfSandboxing.class, "Victory is mine!"));
                assertThat(ex)
                    .isExactlyInstanceOf(SandboxClassLoadingException.class)
                    .hasMessageContaining(Type.getInternalName(SelfSandboxing.class))
                    .hasMessageContaining("Access to sandbox.java.lang.String.toDJVM(String) is forbidden.")
                    .hasMessageContaining("Access to sandbox.java.lang.String.fromDJVM(String) is forbidden.")
                    .hasMessageContaining("Casting to sandbox.java.lang.String is forbidden.")
                    .hasNoCause();
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class SelfSandboxing implements Function<String, String> {
        @SuppressWarnings("ConstantConditions")
        @Override
        public String apply(String message) {
            return (String) (Object) sandbox.java.lang.String.toDJVM(
                sandbox.java.lang.String.fromDJVM((sandbox.java.lang.String) (Object) message)
            );
        }
    }
}
