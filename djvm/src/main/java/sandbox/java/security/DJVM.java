package sandbox.java.security;

import org.jetbrains.annotations.NotNull;

public final class DJVM {
    private DJVM() {}

    @NotNull
    public static <T> java.security.PrivilegedAction<T> fromDJVM(PrivilegedAction<T> action) {
        return new PrivilegedTask<>(action);
    }

    @NotNull
    public static <T> java.security.PrivilegedExceptionAction<T> fromDJVM(PrivilegedExceptionAction<T> action) {
        return new PrivilegedExceptionTask<>(action);
    }

    static final class PrivilegedTask<T> implements java.security.PrivilegedAction<T>, PrivilegedAction<T> {
        private final PrivilegedAction<T> action;

        PrivilegedTask(PrivilegedAction<T> action) {
            this.action = action;
        }

        @Override
        public T run() {
            return action.run();
        }
    }

    static final class PrivilegedExceptionTask<T> implements java.security.PrivilegedExceptionAction<T>, PrivilegedExceptionAction<T> {
        private final PrivilegedExceptionAction<T> action;

        PrivilegedExceptionTask(PrivilegedExceptionAction<T> action) {
            this.action = action;
        }

        @Override
        public T run() throws java.lang.Exception {
            return action.run();
        }
    }
}
