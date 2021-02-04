package net.corda.djvm.execution;

import com.example.testing.BaseObject;
import com.example.testing.DerivedObject;
import net.corda.djvm.ExceptionalConsumer;
import net.corda.djvm.ExceptionalObjectLongConsumer;
import net.corda.djvm.ExceptionalObjectLongIntConsumer;
import net.corda.djvm.TestBase;
import net.corda.djvm.TypedTaskFactory;
import net.corda.djvm.WithJava;
import net.corda.djvm.rewiring.SandboxClassLoader;
import net.corda.djvm.rules.RuleViolationError;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

class JavaObjectTest extends TestBase {
    JavaObjectTest() {
        super(JAVA);
    }

    @Test
    void testOverridingBaseObjectMethods() {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                Class<?> stringClass = classLoader.toSandboxClass(String.class);
                Class<?> sandboxClass = classLoader.toSandboxClass(BaseObject.class);
                validateBaseMethods(sandboxClass, stringClass);
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testInheritingFromSimpleObjectMethods() {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                Class<?> stringClass = classLoader.toSandboxClass(String.class);
                Class<?> sandboxClass = classLoader.toSandboxClass(DerivedObject.class);
                validateBaseMethods(sandboxClass, stringClass);
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    private void validateBaseMethods(Class<?> sandboxClass, Class<?> stringClass) throws NoSuchMethodException {
        Method hashCode = sandboxClass.getMethod("hashCode");
        assertThat(hashCode.getReturnType()).isSameAs(int.class);

        Method toString = sandboxClass.getMethod("toString");
        assertThat(toString.getReturnType()).isSameAs(String.class);

        Method toDJVMString = sandboxClass.getMethod("toDJVMString");
        assertThat(toDJVMString.getReturnType()).isSameAs(stringClass);
    }

    @Test
    void testWaitReferenceIsForbidden() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                RuleViolationError ex = assertThrows(RuleViolationError.class,
                    () -> WithJava.run(taskFactory, ObjectWait.class, null));
                assertThat(ex)
                    .hasMessage("Disallowed reference to API; java.lang.Object.wait()");
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class ObjectWait implements Function<Object, Object> {
        @Override
        public Object apply(Object input) {
            ExceptionalConsumer<? super Object> waiter = Object::wait;
            try {
                waiter.accept(input);
                return null;
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    @Test
    void testWaitLongReferenceIsForbidden() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                RuleViolationError ex = assertThrows(RuleViolationError.class,
                    () -> WithJava.run(taskFactory, ObjectWaitLong.class, null));
                assertThat(ex)
                    .hasMessage("Disallowed reference to API; java.lang.Object.wait(long)");
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class ObjectWaitLong implements Function<Object, Object> {
        @Override
        public Object apply(Object input) {
            ExceptionalObjectLongConsumer<? super Object> waiter = Object::wait;
            try {
                waiter.accept(input, 1000L);
                return null;
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    @Test
    void testWaitLongIntReferenceIsForbidden() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                RuleViolationError ex = assertThrows(RuleViolationError.class,
                    () -> WithJava.run(taskFactory, ObjectWaitLongInt.class, null));
                assertThat(ex)
                    .hasMessage("Disallowed reference to API; java.lang.Object.wait(long, int)");
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class ObjectWaitLongInt implements Function<Object, Object> {
        @Override
        public Object apply(Object input) {
            ExceptionalObjectLongIntConsumer<? super Object> waiter = Object::wait;
            try {
                waiter.accept(input, 1000L, 0);
                return null;
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    @Test
    void testNotifyReferenceIsForbidden() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                RuleViolationError ex = assertThrows(RuleViolationError.class,
                    () -> WithJava.run(taskFactory, ObjectNotify.class, null));
                assertThat(ex)
                    .hasMessage("Disallowed reference to API; java.lang.Object.notify()");
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class ObjectNotify implements Function<Object, Object> {
        @Override
        public Object apply(Object input) {
            Consumer<? super Object> notifier = Object::notify;
            notifier.accept(input);
            return null;
        }
    }

    @Test
    void testNotifyAllReferenceIsForbidden() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                RuleViolationError ex = assertThrows(RuleViolationError.class,
                    () -> WithJava.run(taskFactory, ObjectNotifyAll.class, null));
                assertThat(ex)
                    .hasMessage("Disallowed reference to API; java.lang.Object.notifyAll()");
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class ObjectNotifyAll implements Function<Object, Object> {
        @Override
        public Object apply(Object input) {
            Consumer<? super Object> notifier = Object::notifyAll;
            notifier.accept(input);
            return null;
        }
    }

    @Test
    void testHashCodeReference() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Object result = WithJava.run(taskFactory, GetHashCode.class, new Object());
                assertThat(result).isEqualTo(0xfed_c0de - 1);
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class GetHashCode implements Function<Object, Integer> {
        @Override
        public Integer apply(Object input) {
            ToIntFunction<? super Object> hashCode = Object::hashCode;
            return hashCode.applyAsInt(input);
        }
    }

    @Test
    void testStringReference() {
        UUID uuid = UUID.randomUUID();
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                Class<?> stringClass = classLoader.toSandboxClass(String.class);
                Object result = classLoader.createRawTaskFactory()
                    .compose(classLoader.createSandboxFunction())
                    .apply(GetString.class)
                    .compose(classLoader.createBasicInput())
                    .apply(uuid);
                assertThat(result).isInstanceOf(stringClass);
                assertThat(result.toString()).isEqualTo(uuid.toString());
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class GetString implements Function<Object, String> {
        @Override
        public String apply(Object input) {
            Function<? super Object, String> toString = Object::toString;
            return toString.apply(input);
        }
    }
}
