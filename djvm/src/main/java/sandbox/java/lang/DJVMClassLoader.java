package sandbox.java.lang;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sandbox.java.io.InputStream;
import sandbox.java.net.URL;
import sandbox.java.util.Collections;
import sandbox.java.util.Enumeration;

import java.io.IOException;

/**
 * The DJVM whitelists {@link java.lang.ClassLoader}, which means that it
 * does not transform its references to "sandbox.java.lang.ClassLoader"
 * instead. Happily, {@link java.lang.ClassLoader} does not implement any
 * interfaces, so static references work just fine as a replacement.
 */
@SuppressWarnings("unused")
public final class DJVMClassLoader {
    @NotNull
    public static ClassLoader getSystemClassLoader() {
        return DJVM.getSystemClassLoader();
    }

    @Nullable
    public static URL getSystemResource(String name) {
        return null;
    }

    @SuppressWarnings("RedundantThrows")
    public static Enumeration<URL> getSystemResources(String name) throws IOException {
        return Collections.emptyEnumeration();
    }

    @Nullable
    public static InputStream getSystemResourceAsStream(String name) {
        return null;
    }

    @Nullable
    public static ClassLoader getParent(ClassLoader classLoader) {
        return null;
    }

    @Nullable
    public static URL getResource(ClassLoader classLoader, String name) {
        return null;
    }

    @SuppressWarnings("RedundantThrows")
    public static Enumeration<URL> getResources(ClassLoader classLoader, String name) throws IOException {
        return Collections.emptyEnumeration();
    }

    @Nullable
    public static InputStream getResourceAsStream(ClassLoader classLoader, String name) {
        return null;
    }

    @NotNull
    public static Class<?> loadClass(@NotNull ClassLoader classLoader, String className) throws ClassNotFoundException {
        return classLoader.loadClass(DJVM.toSandbox(className));
    }
}
