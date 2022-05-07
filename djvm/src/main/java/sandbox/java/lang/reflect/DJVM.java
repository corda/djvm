package sandbox.java.lang.reflect;

import org.jetbrains.annotations.NotNull;
import sandbox.java.lang.System;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public final class DJVM {
    private DJVM() {}

    /**
     * The hash-code values for the underlying JVM reflection objects
     * will depend on the JVM's own private implementation, and so we
     * cannot rely on them to be repeatable. Map them to values that
     * we can control instead.
     * @see System#identityHashCode(java.lang.Object)
     */
    private static final int REFLECTION_HASH_OFFSET = 0xaced_c0de;
    private static final Map<java.lang.Integer, java.lang.Integer> hashCodes = new HashMap<>();
    private static int reflectionCounter;

    static int hashCodeFor(int jvmHashCode) {
        return hashCodes.computeIfAbsent(
            jvmHashCode, hash -> ++reflectionCounter + REFLECTION_HASH_OFFSET
        );
    }

    @SuppressWarnings("unused")
    private static void reset(BiConsumer<Object, String> resetter) {
        resetter.accept(new HashMap<>(), "hashCodes");
        reflectionCounter = 0;
    }

    static {
        sandbox.java.lang.DJVM.forReset(MethodHandles.lookup(), "reset");
    }

    public static <T> Constructor<T> toDJVM(java.lang.reflect.Constructor<T> constructor) {
        return constructor != null ? new Constructor<>(constructor) : null;
    }

    @NotNull
    public static Constructor<?>[] toDJVM(@NotNull java.lang.reflect.Constructor<?>[] constructors) {
        Constructor<?>[] result = (Constructor<?>[])java.lang.reflect.Array.newInstance(Constructor.class, constructors.length);
        for (int i = 0; i < constructors.length; ++i) {
            result[i] = toDJVM(constructors[i]);
        }
        return result;
    }

    public static Method toDJVM(java.lang.reflect.Method method) {
        return method != null ? new Method(method) : null;
    }

    @NotNull
    public static Method[] toDJVM(@NotNull java.lang.reflect.Method[] methods) {
        Method[] result = (Method[])java.lang.reflect.Array.newInstance(Method.class, methods.length);
        for (int i = 0; i < methods.length; ++i) {
            result[i] = toDJVM(methods[i]);
        }
        return result;
    }

    public static Field toDJVM(java.lang.reflect.Field field) {
        return field != null ? new Field(field) : null;
    }

    @NotNull
    public static Field[] toDJVM(@NotNull java.lang.reflect.Field[] fields) {
        Field[] result = (Field[])java.lang.reflect.Array.newInstance(Field.class, fields.length);
        for (int i = 0; i < fields.length; ++i) {
            result[i] = toDJVM(fields[i]);
        }
        return result;
    }
}
