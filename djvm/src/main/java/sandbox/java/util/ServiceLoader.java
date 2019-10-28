package sandbox.java.util;

import sandbox.java.lang.Iterable;
import sandbox.java.lang.String;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public final class ServiceLoader<T> extends sandbox.java.lang.Object implements Iterable<T> {
    private final Class<T> service;

    private ServiceLoader(Class<T> service, ClassLoader cl) {
        this.service = service;
    }

    public void reload() {}

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return Collections.emptyIterator();
    }

    @NotNull
    public static <S> ServiceLoader<S> load(Class<S> service, ClassLoader loader) {
        return new ServiceLoader<>(service, loader);
    }

    @NotNull
    public static <S> ServiceLoader<S> load(Class<S> service) {
        return ServiceLoader.load(service, null);
    }

    @NotNull
    public static <S> ServiceLoader<S> loadInstalled(Class<S> service) {
        return ServiceLoader.load(service, null);
    }

    @Override
    @NotNull
    public String toDJVMString() {
        return String.toDJVM("java.util.ServiceLoader[" + service.getName() + ']');
    }
}
