package sandbox.java.lang;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sandbox.java.io.InputStream;
import sandbox.java.lang.annotation.Annotation;
import sandbox.java.lang.reflect.AnnotatedType;
import sandbox.java.lang.reflect.Constructor;
import sandbox.java.lang.reflect.Field;
import sandbox.java.lang.reflect.Method;
import sandbox.java.lang.reflect.Type;
import sandbox.java.lang.reflect.TypeVariable;
import sandbox.java.net.URL;
import sandbox.java.security.ProtectionDomain;
import sandbox.java.util.LinkedHashMap;
import sandbox.java.util.Map;

import java.lang.invoke.MethodHandles;
import java.security.PrivilegedActionException;
import java.util.Arrays;
import java.util.function.BiConsumer;

import static java.security.AccessController.doPrivileged;
import static sandbox.java.util.Collections.unmodifiableMap;
import static org.objectweb.asm.Opcodes.ACC_ENUM;

/**
 * The DJVM whitelists {@link java.lang.Class}, which means that it does not
 * transform its references to "sandbox.java.lang.Class" instead. This is a
 * Good Idea(tm) for the most part, but does also mean that classes cannot
 * implement the {@link java.lang.reflect.GenericDeclaration} or
 * {@link java.lang.reflect.Type} interfaces properly inside the sandbox.
 */
@SuppressWarnings("unused")
public final class DJVMClass {
    private static final java.lang.String FROM_DJVM = "fromDJVM";
    private static final Map<Class<? extends Enum<?>>, Enum<?>[]> allEnums = new LinkedHashMap<>();
    private static final Map<Class<? extends Enum<?>>, Map<String, ? extends Enum<?>>> allEnumDirectories = new LinkedHashMap<>();

    private DJVMClass() {}

    @SuppressWarnings("unused")
    private static void reset(BiConsumer<java.lang.Object, java.lang.String> resetter) {
        resetter.accept(new LinkedHashMap<>(), "allEnumDirectories");
        resetter.accept(new LinkedHashMap<>(), "allEnums");
    }

    static {
        DJVM.forReset(MethodHandles.lookup(), "reset");
    }

    @NotNull
    public static Class<?> forName(String className) throws ClassNotFoundException {
        return DJVM.classForName(className, true);
    }

    @NotNull
    public static Class<?> forName(String className, boolean initialize, ClassLoader classLoader) throws ClassNotFoundException {
        return DJVM.classForName(className, initialize);
    }

    /**
     * Filter function for {@link Class#getClassLoader}.
     * We perform no "access control" checks because we are pretending
     * that all sandbox classes exist inside the same classloader.
     *
     * We would expect {@link Class#getClassLoader} to return one of the following:
     * - {@link net.corda.djvm.rewiring.SandboxClassLoader} for sandbox classes
     * - The application class loader for whitelisted classes
     * - {@literal null} for basic Java classes.
     *
     * So "don't do that". Always return the sandbox classloader instead.
     *
     * @param clazz The class that we are pretending to query.
     * @return Always the "top-most" sandbox classloader.
     */
    @NotNull
    public static ClassLoader getClassLoader(Class<?> clazz) {
        return DJVM.getSystemClassLoader();
    }

    public static String toString(@NotNull Class<?> clazz) {
        return String.toDJVM(clazz.toString());
    }

    public static String getName(@NotNull Class<?> clazz) {
        return String.toDJVM(clazz.getName());
    }

    public static String getCanonicalName(@NotNull Class<?> clazz) {
        return String.toDJVM(clazz.getCanonicalName());
    }

    public static String getSimpleName(@NotNull Class<?> clazz) {
        return String.toDJVM(clazz.getSimpleName());
    }

    public static String toGenericString(@NotNull Class<?> clazz) {
        return String.toDJVM(clazz.toGenericString());
    }

    public static String getTypeName(@NotNull Class<?> clazz) {
        return String.toDJVM(clazz.getTypeName());
    }

    public static Class<?>[] getDeclaredClasses(Class<?> clazz) {
        throw DJVM.failApi("java.lang.Class.getDeclaredClasses()");
    }

    @Nullable
    public static java.lang.Object[] getSigners(Class<?> clazz) {
        return null;
    }

    /*
     * These reflection APIs are "@CallerSensitive" so that a SecurityManager
     * can determine whether the caller should be permitted to query the class.
     * For the sake of repeatability, we must assume that the caller should
     * always be able to query sandbox classes. Note also that the true caller
     * has become DJVMClass itself now, and this should satisfy a SecurityManager
     * because DJVMClass lives in the "base" SandboxClassLoader. (A classloader
     * is permitted to query classes belonging to its children.)
     */
    @SuppressWarnings("RedundantThrows")
    public static <T> Constructor<T> getDeclaredConstructor(Class<T> clazz, Class<?>[] parameterTypes) throws NoSuchMethodException {
        throw DJVM.failApi("java.lang.Class.getDeclaredConstructor(Class[])");
    }

    public static Constructor<?>[] getDeclaredConstructors(Class<?> clazz) {
        throw DJVM.failApi("java.lang.Class.getDeclaredConstructors()");
    }

    public static Constructor<?> getEnclosingConstructor(@NotNull Class<?> clazz) {
        return sandbox.java.lang.reflect.DJVM.toDJVM(clazz.getEnclosingConstructor());
    }

    public static <T> Constructor<T> getConstructor(@NotNull Class<T> clazz, Class<?>[] parameterTypes) throws NoSuchMethodException {
        java.lang.reflect.Constructor<T> constructor = clazz.getConstructor(parameterTypes);
        return sandbox.java.lang.reflect.DJVM.toDJVM(constructor);
    }

    @NotNull
    public static Constructor<?>[] getConstructors(@NotNull Class<?> clazz) {
        return sandbox.java.lang.reflect.DJVM.toDJVM(clazz.getConstructors());
    }

    @SuppressWarnings("RedundantThrows")
    public static Method getDeclaredMethod(Class<?> clazz, String methodName, Class<?>[] parameterTypes) throws NoSuchMethodException {
        throw DJVM.failApi("java.lang.Class.getDeclaredMethod(String, Class[])");
    }

    public static Method[] getDeclaredMethods(Class<?> clazz) {
        throw DJVM.failApi("java.lang.Class.getDeclaredMethods()");
    }

    public static Method getEnclosingMethod(@NotNull Class<?> clazz) {
        return sandbox.java.lang.reflect.DJVM.toDJVM(clazz.getEnclosingMethod());
    }

    private static boolean isToString(java.lang.String name, Class<?>[] parameterTypes) {
        return "toString".equals(name) && parameterTypes.length == 0;
    }

    private static boolean isPermittedMethod(@NotNull java.lang.reflect.Method method) {
        java.lang.String javaName = method.getName();
        return !FROM_DJVM.equals(javaName) && !isToString(javaName, method.getParameterTypes());
    }

    public static Method getMethod(@NotNull Class<?> clazz, String methodName, Class<?>[] parameterTypes) throws NoSuchMethodException {
        java.lang.String javaName = String.fromDJVM(methodName);
        if (isToString(javaName, parameterTypes)) {
            javaName = "toDJVMString";
        } else if (FROM_DJVM.equals(javaName)) {
            throw new NoSuchMethodException(javaName);
        }
        java.lang.reflect.Method method = clazz.getMethod(javaName, parameterTypes);
        return sandbox.java.lang.reflect.DJVM.toDJVM(method);
    }

    @NotNull
    public static Method[] getMethods(@NotNull Class<?> clazz) {
        return sandbox.java.lang.reflect.DJVM.toDJVM(
            Arrays.stream(clazz.getMethods())
                .filter(DJVMClass::isPermittedMethod)
                .toArray(java.lang.reflect.Method[]::new)
        );
    }

    @SuppressWarnings("RedundantThrows")
    public static Field getDeclaredField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        throw DJVM.failApi("java.lang.Class.getDeclaredField(String)");
    }

    public static Field[] getDeclaredFields(Class<?> clazz) {
        throw DJVM.failApi("java.lang.Class.getDeclaredFields()");
    }

    public static Field getField(@NotNull Class<?> clazz, String fieldName) throws NoSuchFieldException {
        java.lang.reflect.Field field = clazz.getField(String.fromDJVM(fieldName));
        return sandbox.java.lang.reflect.DJVM.toDJVM(field);
    }

    @NotNull
    public static Field[] getFields(@NotNull Class<?> clazz) {
        return sandbox.java.lang.reflect.DJVM.toDJVM(clazz.getFields());
    }

    @Nullable
    public static Package getPackage(Class<?> clazz) {
        return null;
    }

    public static TypeVariable<?>[] getTypeParameters(Class<?> clazz) {
        throw DJVM.failApi("java.lang.Class.getTypeParameters()");
    }

    public static AnnotatedType[] getAnnotatedInterfaces(Class<?> clazz) {
        throw DJVM.failApi("java.lang.Class.getAnnotatedInterfaces()");
    }

    public static AnnotatedType getAnnotatedSuperclass(Class<?> clazz) {
        throw DJVM.failApi("java.lang.Class.getAnnotatedSuperclass()");
    }

    public static Type[] getGenericInterfaces(Class<?> clazz) {
        throw DJVM.failApi("java.lang.Class.getGenericInterfaces()");
    }

    public static Type getGenericSuperclass(Class<?> clazz) {
        throw DJVM.failApi("java.lang.Class.getGenericSuperclass()");
    }

    public static ProtectionDomain getProtectionDomain(Class<?> clazz) {
        throw DJVM.failApi("java.lang.Class.getProtectionDomain()");
    }

    @Nullable
    public static InputStream getResourceAsStream(Class<?> clazz, String name) {
        return null;
    }

    @Nullable
    public static URL getResource(Class<?> clazz, String name) {
        return null;
    }

    /*
     * Support for Class annotations.
     */
    public static boolean isAnnotationPresent(@NotNull Class<?> clazz, Class<? extends Annotation> annotationType) {
        return DJVM.isAnnotationPresent(clazz, annotationType);
    }

    public static Annotation getAnnotation(@NotNull Class<?> clazz, Class<? extends Annotation> annotationType) {
        return DJVM.getAnnotation(clazz, annotationType);
    }

    @NotNull
    public static Annotation[] getAnnotations(@NotNull Class<?> clazz) {
        return DJVM.getAnnotations(clazz);
    }

    @NotNull
    public static Annotation[] getAnnotationsByType(@NotNull Class<?> clazz, Class<? extends Annotation> annotationType) {
        return DJVM.getAnnotationsByType(clazz, annotationType);
    }

    public static Annotation getDeclaredAnnotation(@NotNull Class<?> clazz, Class<? extends Annotation> annotationType) {
        return DJVM.getDeclaredAnnotation(clazz, annotationType);
    }

    @NotNull
    public static Annotation[] getDeclaredAnnotations(@NotNull Class<?> clazz) {
        return DJVM.getDeclaredAnnotations(clazz);
    }

    @NotNull
    public static Annotation[] getDeclaredAnnotationsByType(@NotNull Class<?> clazz, Class<? extends Annotation> annotationType) {
        return DJVM.getDeclaredAnnotationsByType(clazz, annotationType);
    }

    /*
     * Support for Enum types.
     */
    public static boolean isEnum(@NotNull Class<?> clazz) {
        return (clazz.getModifiers() & ACC_ENUM) != 0 && (clazz.getSuperclass() == Enum.class);
    }

    @Nullable
    public static java.lang.Object[] getEnumConstants(Class<? extends Enum<?>> clazz) {
        Enum<?>[] constants = getEnumConstantsShared(clazz);
        return constants != null ? constants.clone() : null;
    }

    @NotNull
    static Map<String, ? extends Enum<?>> enumConstantDirectory(Class<? extends Enum<?>> clazz) {
        Map<String, ? extends Enum<?>> enums = allEnumDirectories.get(clazz);
        return enums != null ? enums : createEnumDirectory(clazz);
    }

    @Nullable
    static Enum<?>[] getEnumConstantsShared(Class<? extends Enum<?>> clazz) {
        if (isEnum(clazz)) {
            Enum<?>[] enums = allEnums.get(clazz);
            return enums != null ? enums : createEnum(clazz);
        }
        return null;
    }

    @NotNull
    private static Map<String, ? extends Enum<?>> createEnumDirectory(Class<? extends Enum<?>> clazz) {
        Enum<?>[] universe = getEnumConstantsShared(clazz);
        if (universe == null) {
            throw new IllegalArgumentException(clazz.getName() + " is not an enum type");
        }
        Map<String, Enum<?>> directory = new LinkedHashMap<>(2 * universe.length);
        for (Enum<?> entry : universe) {
            directory.put(entry.name(), entry);
        }
        allEnumDirectories.put(clazz, unmodifiableMap(directory));
        return directory;
    }

    private static Enum<?>[] createEnum(Class<? extends Enum<?>> clazz) {
        try {
            Enum<?>[] enums = doPrivileged(new DJVMEnumAction(clazz));
            allEnums.put(clazz, enums);
            return enums;
        } catch (PrivilegedActionException e) {
            throw DJVM.toRuleViolationError(e.getCause());
        }
    }
}
