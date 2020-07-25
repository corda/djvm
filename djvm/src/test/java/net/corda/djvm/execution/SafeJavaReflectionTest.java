package net.corda.djvm.execution;

import net.corda.djvm.ExceptionalFunction;
import net.corda.djvm.TestBase;
import net.corda.djvm.TypedTaskFactory;
import net.corda.djvm.WithJava;
import net.corda.djvm.rules.RuleViolationError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

class SafeJavaReflectionTest extends TestBase {
    SafeJavaReflectionTest() {
        super(JAVA);
    }

    @Test
    void testGetClasses() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String[] result = WithJava.run(taskFactory, GetClassClasses.class, null);
                assertThat(result).containsExactlyInAnyOrder(
                    "sandbox.net.corda.djvm.execution.GetClassClasses$NestedException"
                );
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testGetDeclaredClasses() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                RuleViolationError ex = assertThrows(RuleViolationError.class,
                    () -> WithJava.run(taskFactory, GetClassDeclaredClasses.class, null));
                assertThat(ex)
                    .hasMessage("Disallowed reference to API; java.lang.Class.getDeclaredClasses()")
                    .hasNoCause();
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testInvokingConstructor() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                RuleViolationError ex = assertThrows(RuleViolationError.class,
                    () -> WithJava.run(taskFactory, InvokeConstructor.class, null));
                assertThat(ex)
                    .hasMessage("Disallowed reference to API; java.lang.reflect.Constructor.newInstance(Object[])")
                    .hasNoCause();
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class InvokeConstructor implements Function<String, String> {
        @Override
        public String apply(String data) {
            try {
                UserData userData = UserData.class.getConstructor(String.class).newInstance(data);
                return userData.toString();
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    @Test
    void testInvokingDeclaredConstructor() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                RuleViolationError ex = assertThrows(RuleViolationError.class,
                    () -> WithJava.run(taskFactory, InvokeDeclaredConstructor.class, null));
                assertThat(ex)
                    .hasMessage("Disallowed reference to API; java.lang.Class.getDeclaredConstructor(Class[])")
                    .hasNoCause();
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class InvokeDeclaredConstructor implements Function<String, String> {
        @Override
        public String apply(String data) {
            try {
                UserData userData = UserData.class.getDeclaredConstructor(String.class).newInstance(data);
                return userData.toString();
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    @Test
    void testInvokingNewInstanceByReference() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                RuleViolationError ex = assertThrows(RuleViolationError.class,
                    () -> WithJava.run(taskFactory, InvokeNewInstanceByReference.class, null));
                assertThat(ex)
                    .hasMessage("Disallowed reference to API; java.lang.reflect.Constructor.newInstance(Object...)")
                    .hasNoCause();
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class InvokeNewInstanceByReference implements Function<String, String> {
        @Override
        public String apply(String data) {
            ExceptionalFunction<Object[], UserData> factory;
            Constructor<UserData> constructor;
            try {
                constructor = UserData.class.getConstructor(String.class);
                factory = constructor::newInstance;
                return factory.apply(new Object[]{ data }).toString();
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    @Test
    void testInvokingMethod() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                RuleViolationError ex = assertThrows(RuleViolationError.class,
                     () -> WithJava.run(taskFactory, InvokeMethod.class, null));
                assertThat(ex)
                    .hasMessage("Disallowed reference to API; java.lang.reflect.Method.invoke(Object, Object...)")
                    .hasNoCause();
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class InvokeMethod implements Function<String, String> {
        public String getMessage() {
            return "Invoked!";
        }

        @Override
        public String apply(String unused) {
            try {
                return (String)getClass().getMethod("getMessage").invoke(this);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    @Test
    void testInvokingDeclaredMethod() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                RuleViolationError ex = assertThrows(RuleViolationError.class,
                    () -> WithJava.run(taskFactory, InvokeDeclaredMethod.class, null));
                assertThat(ex)
                    .hasMessage("Disallowed reference to API; java.lang.Class.getDeclaredMethod(String, Class[])")
                    .hasNoCause();
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class InvokeDeclaredMethod implements Function<String, String> {
        public String getMessage() {
            return "Invoked Method!";
        }

        @Override
        public String apply(String unused) {
            try {
                return (String)(getClass().getDeclaredMethod("getMessage").invoke(this));
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    @Test
    void testGetMethods() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String[] methodNames = WithJava.run(taskFactory, GetMethods.class, null);
                assertThat(methodNames).containsExactlyInAnyOrder(
                    "andThen",
                    "apply", "apply",
                    "compose",
                    "equals",
                    "getClass",
                    "getPublicMessage",
                    "hashCode",
                    "notify",
                    "notifyAll",
                    "toString",
                    "wait", "wait", "wait"
                );
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    @SuppressWarnings("unused")
    public static class GetMethods implements Function<String, String[]> {
        public String getPublicMessage() {
            return "Public Method";
        }

        protected String getProtectedMessage() {
            return "Protected Method";
        }

        String getPackageMessage() {
            return "Package Method";
        }

        private String getPrivateMessage() {
            return "Private Method";
        }

        @Override
        public String[] apply(String unused) {
            return Arrays.stream(getClass().getMethods())
                .map(Method::getName)
                .toArray(String[]::new);
        }
    }

    @ParameterizedTest
    @CsvSource({
        "toString,sandbox.java.lang.String,public sandbox.java.lang.String sandbox.java.lang.Object.toString()",
        "hashCode,int, public int sandbox.java.lang.Object.hashCode()",
        "notify,void,public final native void java.lang.Object.notify()",
        "notifyAll,void,public final native void java.lang.Object.notifyAll()",
        "wait,void,public final void java.lang.Object.wait() throws java.lang.InterruptedException"
    })
    void testGetMethod(String methodName, String returnType, String toStringValue) {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String[] results = WithJava.run(taskFactory, GetMethod.class, methodName);
                assertThat(results).containsExactly(
                    methodName,
                    returnType,
                    toStringValue
                );
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class GetMethod implements Function<String, String[]> {
        @Override
        public String[] apply(String methodName) {
            Method method;
            try {
                method = getClass().getMethod(methodName);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            return new String[] {
                method.getName(),
                method.getReturnType().getName(),
                method.toString()
            };
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class UserData {
        private final String data;
        public final String publicData;

        public UserData(String data) {
            this.data = data;
            this.publicData = data;
        }

        @Override
        public String toString() {
            return data;
        }
    }

    @Test
    void testWithEnclosingConstructor() {
        assertThat(new WithEnclosingConstructor().apply(null)).isNotNull();
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Constructor result = WithJava.run(taskFactory, WithEnclosingConstructor.class, null);
                assertThat(result).isNotNull();
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    @SuppressWarnings("WeakerAccess")
    public static class WithEnclosingConstructor implements Function<String, Constructor<?>> {
        private final Constructor<?> enclosed;

        public WithEnclosingConstructor() {
            class Enclosed {}
            enclosed = Enclosed.class.getEnclosingConstructor();
        }

        @Override
        public Constructor<?> apply(String unused) {
            return enclosed;
        }
    }

    @Test
    void testWithoutEnclosingConstructor() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Constructor result = WithJava.run(taskFactory, WithoutEnclosingConstructor.class, null);
                assertThat(result).isNull();
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class WithoutEnclosingConstructor implements Function<String, Constructor<?>> {
        @Override
        public Constructor<?> apply(String unused) {
            return UserData.class.getEnclosingConstructor();
        }
    }

    @Test
    void testWithEnclosingMethod() {
        assertThat(new WithEnclosingMethod().apply(null)).isNotNull();
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Method result = WithJava.run(taskFactory, WithEnclosingMethod.class, null);
                assertThat(result).isNotNull();
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class WithEnclosingMethod implements Function<String, Method> {
        @Override
        public Method apply(String unused) {
            class Enclosed {}
            return Enclosed.class.getEnclosingMethod();
        }
    }

    @Test
    void testWithoutEnclosingMethod() {
        assertThat(new WithoutEnclosingMethod().apply(null)).isNull();
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Method result = WithJava.run(taskFactory, WithoutEnclosingMethod.class, null);
                assertThat(result).isNull();
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class WithoutEnclosingMethod implements Function<String, Method> {
        @Override
        public Method apply(String unused) {
            return UserData.class.getEnclosingMethod();
        }
    }

    @Test
    void testGetFields() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String[] fieldNames = WithJava.run(taskFactory, GetFields.class, null);
                assertThat(fieldNames).containsExactlyInAnyOrder(
                    "publicData"
                );
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class GetFields implements Function<String, String[]> {
        @Override
        public String[] apply(String unused) {
            return Arrays.stream(UserData.class.getFields())
                .map(Field::getName)
                .toArray(String[]::new);
        }
    }

    @ParameterizedTest
    @CsvSource({
        "bigNumber,sandbox.java.lang.Long," + ACC_PUBLIC,
        "CHARACTER,char," + (ACC_PUBLIC | ACC_STATIC),
        "MESSAGE,sandbox.java.lang.String," + (ACC_PUBLIC | ACC_STATIC | ACC_FINAL),
        "flag,boolean," + (ACC_PUBLIC | ACC_FINAL),
        "number,int," + ACC_PUBLIC
    })
    void testGetField(String fieldName, String typeName, int modifiers) {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Object[] results = WithJava.run(taskFactory, GetField.class, fieldName);
                assertThat(results).containsExactly(
                    fieldName,
                    typeName,
                    modifiers
                );
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    @SuppressWarnings("unused")
    public static class GetField implements Function<String, Object[]> {
        public static final String MESSAGE = "Hello Field!";
        public static char CHARACTER;
        public final boolean flag = true;
        public Long bigNumber;
        public int number;

        @Override
        public Object[] apply(String fieldName) {
            Field field;
            try {
                field = getClass().getField(fieldName);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            return new Object[] {
                field.getName(),
                field.getType().getName(),
                field.getModifiers()
            };
        }
    }

    @Test
    void testGetFieldValues() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Object[] results = WithJava.run(taskFactory, GetFieldValues.class, null);
                assertThat(results).containsExactly(
                    "Hello Field!",
                    12345678L,
                    123456,
                    true,
                    (short) 1234,
                    (byte) 136,
                    '\u267b',
                    1234.56f,
                    123456.7899d
                );
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    @SuppressWarnings("WeakerAccess")
    public static class GetFieldValues implements Function<String, Object[]> {
        public final String message = "Hello Field!";
        public final long bigNumber = 12345678L;
        public final int number = 123456;
        public final boolean flag = true;
        public final short littleNumber = (short) 1234;
        public final byte tinyNumber = (byte) 136;
        public final char character = '\u267b';
        public final float realNumber = 1234.56f;
        public final double bigRealNumber = 123456.7899d;

        @Override
        public Object[] apply(String unused) {
            try {
                return new Object[] {
                    GetFieldValues.class.getField("message").get(this),
                    GetFieldValues.class.getField("bigNumber").getLong(this),
                    GetFieldValues.class.getField("number").getInt(this),
                    GetFieldValues.class.getField("flag").getBoolean(this),
                    GetFieldValues.class.getField("littleNumber").getShort(this),
                    GetFieldValues.class.getField("tinyNumber").getByte(this),
                    GetFieldValues.class.getField("character").getChar(this),
                    GetFieldValues.class.getField("realNumber").getFloat(this),
                    GetFieldValues.class.getField("bigRealNumber").getDouble(this),
                };
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    @Test
    void testGetDeclaredField() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                RuleViolationError ex = assertThrows(RuleViolationError.class,
                    () -> WithJava.run(taskFactory, GetDeclaredField.class, "publicData")
                );
                assertThat(ex)
                    .hasMessage("Disallowed reference to API; java.lang.Class.getDeclaredField(String)")
                    .hasNoCause();
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class GetDeclaredField implements Function<String, Field> {
        @Override
        public Field apply(String fieldName) {
            try {
                return UserData.class.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    @Test
    void testDeclaredFields() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                RuleViolationError ex = assertThrows(RuleViolationError.class,
                    () -> WithJava.run(taskFactory, GetDeclaredFields.class, null)
                );
                assertThat(ex)
                    .hasMessage("Disallowed reference to API; java.lang.Class.getDeclaredFields()")
                    .hasNoCause();
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class GetDeclaredFields implements Function<String, Field[]> {
        @Override
        public Field[] apply(String unused) {
            return UserData.class.getDeclaredFields();
        }
    }
}