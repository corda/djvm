package net.corda.djvm.execution;

import com.example.testing.BaseObject;
import com.example.testing.ConcreteObject;
import com.example.testing.GenericObject;
import net.corda.djvm.TestBase;
import net.corda.djvm.rewiring.SandboxClassLoader;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ObjectArrayAssert;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.Arrays;

import static net.corda.djvm.SandboxType.JAVA;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class SandboxObjectTest extends TestBase {
    SandboxObjectTest() {
        super(JAVA);
    }

    @Test
    void testSimpleObjectReparenting() {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                Class<?> sandboxParentClass = classLoader.loadClass("sandbox.java.lang.Object");
                Class<?> sandboxClass = classLoader.toSandboxClass(BaseObject.class);
                assertAll(
                    () -> assertEquals("sandbox.com.example.testing.BaseObject", sandboxClass.getName()),
                    () -> assertEquals("BaseObject", sandboxClass.getSimpleName()),
                    () -> assertEquals("sandbox.com.example.testing.BaseObject", sandboxClass.getCanonicalName()),
                    () -> assertEquals("sandbox.com.example.testing.BaseObject", sandboxClass.getTypeName()),
                    () -> assertEquals("class sandbox.com.example.testing.BaseObject", sandboxClass.toString()),
                    () -> assertEquals("public class sandbox.com.example.testing.BaseObject", sandboxClass.toGenericString()),
                    () -> assertEquals("sandbox.java.lang.Object", sandboxClass.getGenericSuperclass().getTypeName()),
                    () -> assertEquals("sandbox.java.lang.Object", sandboxClass.getSuperclass().getName()),
                    () -> assertTrue(sandboxParentClass.isAssignableFrom(sandboxClass)),
                    () -> assertThat(sandboxClass.getGenericInterfaces()).isEmpty()
                );
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testGenericObjectReparenting() {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                Class<?> sandboxParentClass = classLoader.loadClass("sandbox.java.lang.Object");
                Class<?> sandboxClass = classLoader.toSandboxClass(GenericObject.class);
                assertAll(
                    () -> assertEquals("sandbox.com.example.testing.GenericObject", sandboxClass.getName()),
                    () -> assertEquals("GenericObject", sandboxClass.getSimpleName()),
                    () -> assertEquals("sandbox.com.example.testing.GenericObject", sandboxClass.getCanonicalName()),
                    () -> assertEquals("sandbox.com.example.testing.GenericObject", sandboxClass.getTypeName()),
                    () -> assertEquals("class sandbox.com.example.testing.GenericObject", sandboxClass.toString()),
                    () -> assertEquals("public abstract class sandbox.com.example.testing.GenericObject<T,R>", sandboxClass.toGenericString()),
                    () -> assertEquals("sandbox.java.lang.Object", sandboxClass.getGenericSuperclass().getTypeName()),
                    () -> assertEquals("sandbox.java.lang.Object", sandboxClass.getSuperclass().getName()),
                    () -> assertTrue(sandboxParentClass.isAssignableFrom(sandboxClass)),
                    () -> assertThat(sandboxClass.getGenericInterfaces()).containsExactly("sandbox.java.util.function.Function<T, R>")
                );
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testConcreteObjectReparenting() {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                Class<?> sandboxParentClass = classLoader.loadClass("sandbox.com.example.testing.GenericObject");
                Class<?> sandboxClass = classLoader.toSandboxClass(ConcreteObject.class);
                assertAll(
                    () -> assertEquals("sandbox.com.example.testing.ConcreteObject", sandboxClass.getName()),
                    () -> assertEquals("ConcreteObject", sandboxClass.getSimpleName()),
                    () -> assertEquals("sandbox.com.example.testing.ConcreteObject", sandboxClass.getCanonicalName()),
                    () -> assertEquals("sandbox.com.example.testing.ConcreteObject", sandboxClass.getTypeName()),
                    () -> assertEquals("class sandbox.com.example.testing.ConcreteObject", sandboxClass.toString()),
                    () -> assertEquals("public class sandbox.com.example.testing.ConcreteObject", sandboxClass.toGenericString()),
                    () -> assertEquals("sandbox.com.example.testing.GenericObject<sandbox.java.lang.Long, sandbox.java.lang.String>",
                                       sandboxClass.getGenericSuperclass().getTypeName()),
                    () -> assertEquals("sandbox.com.example.testing.GenericObject", sandboxClass.getSuperclass().getName()),
                    () -> assertTrue(sandboxParentClass.isAssignableFrom(sandboxClass)),
                    () -> assertThat(sandboxClass.getGenericInterfaces()).isEmpty()
                );
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    @NotNull
    private static ObjectArrayAssert<String> assertThat(Type[] types) {
        return Assertions.assertThat(
            Arrays.stream(types).map(Type::getTypeName).toArray(String[]::new)
        );
    }
}
