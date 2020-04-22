package net.corda.djvm.code;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;

/**
 * These are the names of methods in {@link sandbox.java.lang.DJVMClass}
 * and {@link sandbox.java.lang.DJVMClassLoader}. They correspond to the
 * {@link java.lang.Class} and {@link java.lang.ClassLoader} methods that
 * we intercept.
 *
 * Package private so that the elements remain visible for testing.
 */
final class Thunks {
    private static final Set<String> CLASS_VIRTUAL;
    private static final Set<String> CLASSLOADER_VIRTUAL;
    private static final Set<String> CLASSLOADER_STATIC;

    static {
        Set<String> classVirtual = new HashSet<>();
        Collections.addAll(classVirtual,
            // We only need to intercept this when we sandbox
            // java.lang.Enum because it is package private.
            "enumConstantDirectory",

            // These are all public methods.
            "getAnnotation",
            "getAnnotations",
            "getAnnotationsByType",
            "getCanonicalName",
            "getClassLoader",
            "getDeclaredAnnotation",
            "getDeclaredAnnotations",
            "getDeclaredAnnotationsByType",
            "getDeclaredClasses",
            "getEnumConstants",
            "getName",
            "getPackage",
            "getProtectionDomain",
            "getResource",
            "getResourceAsStream",
            "getSimpleName",
            "getTypeName",
            "isAnnotationPresent",
            "isEnum",
            "toGenericString",
            "toString"
        );
        CLASS_VIRTUAL = unmodifiableSet(classVirtual);

        Set<String> classloaderVirtual = new HashSet<>();
        Collections.addAll(classloaderVirtual,
            "getParent",
            "getResource",
            "getResources",
            "getResourceAsStream",
            "loadClass"
        );
        CLASSLOADER_VIRTUAL = unmodifiableSet(classloaderVirtual);

        Set<String> classloaderStatic = new HashSet<>();
        Collections.addAll(classloaderStatic,
            "getSystemClassLoader",
            "getSystemResource",
            "getSystemResources",
            "getSystemResourceAsStream"
        );
        CLASSLOADER_STATIC = unmodifiableSet(classloaderStatic);
    }

    private Thunks() {}

    static boolean isClassVirtual(String methodName) {
        return CLASS_VIRTUAL.contains(methodName);
    }

    static boolean isClassLoaderVirtual(String methodName) {
        return CLASSLOADER_VIRTUAL.contains(methodName);
    }

    static boolean isClassLoaderStatic(String methodName) {
        return CLASSLOADER_STATIC.contains(methodName);
    }

    static Set<String> getClassVirtual() {
        return CLASS_VIRTUAL;
    }

    static Set<String> getClassLoaderVirtual() {
        return CLASSLOADER_VIRTUAL;
    }

    static Set<String> getClassLoaderStatic() {
        return CLASSLOADER_STATIC;
    }
}
