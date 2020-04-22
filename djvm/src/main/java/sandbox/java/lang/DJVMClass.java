package sandbox.java.lang;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sandbox.java.io.InputStream;
import sandbox.java.lang.annotation.Annotation;
import sandbox.java.net.URL;
import sandbox.java.security.ProtectionDomain;
import sandbox.java.util.LinkedHashMap;
import sandbox.java.util.Map;

import java.security.PrivilegedActionException;

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
    private static final Map<Class<? extends Enum<?>>, Enum<?>[]> allEnums = new LinkedHashMap<>();
    private static final Map<Class<? extends Enum<?>>, Map<String, ? extends Enum<?>>> allEnumDirectories = new LinkedHashMap<>();

    private DJVMClass() {}

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
     */
    @NotNull
    public static ClassLoader getClassLoader(Class<?> clazz) {
        return DJVM.getSystemClassLoader();
    }

    @SuppressWarnings("RedundantThrows")
    public static Class<?> forName(String className) throws ClassNotFoundException {
        throw DJVM.failApi("java.lang.Class.forName(String)");
    }

    @SuppressWarnings("RedundantThrows")
    public static Class<?> forName(String className, boolean initialize, ClassLoader classLoader) throws ClassNotFoundException {
        throw DJVM.failApi("java.lang.Class.forName(String,boolean,ClassLoader)");
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
    public static Package getPackage(Class<?> clazz) {
        return null;
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
