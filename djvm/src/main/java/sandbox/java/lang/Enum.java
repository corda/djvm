package sandbox.java.lang;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * This is a dummy class that implements just enough of {@link java.lang.Enum}
 * to allow us to compile {@link sandbox.java.lang.DJVM}.
 */
@SuppressWarnings("unused")
public abstract class Enum<E extends Enum<E>> extends Object implements Comparable<E>, Serializable {
    private static final java.lang.String UNSUPPORTED = "Dummy class - not implemented";

    private final String name;
    private final int ordinal;

    protected Enum(String name, int ordinal) {
        this.name = name;
        this.ordinal = ordinal;
    }

    public String name() {
        return name;
    }

    public int ordinal() {
        return ordinal;
    }

    public static <T extends Enum<T>> T valueOf(Class<T> enumType, String name) {
        throw new UnsupportedOperationException(UNSUPPORTED);
    }

    @Override
    @NotNull
    protected final java.lang.Enum<?> fromDJVM() {
        throw new UnsupportedOperationException(UNSUPPORTED);
    }
}
