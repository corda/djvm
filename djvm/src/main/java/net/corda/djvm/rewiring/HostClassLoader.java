package net.corda.djvm.rewiring;

/**
 * A package-private {@link ClassLoader} for efficient access to
 * the DJVM's own classes and everything else on the classpath.
 */
class HostClassLoader extends ClassLoader {
    private final ClassLoader djvmClassLoader;

    HostClassLoader() {
        super(null);
        djvmClassLoader = HostClassLoader.class.getClassLoader();
    }

    @Override
    protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
        /*
         * Using Class.forName() because otherwise multiple sandbox
         * threads would fight over the Application ClassLoader's
         * class-loading lock. Instead, we consult the JVM's own
         * "lockless" cache of classes that have been loaded already.
         */
        return Class.forName(className, false, djvmClassLoader);
    }
}
