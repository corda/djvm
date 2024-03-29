package sandbox.java.lang;

import org.jetbrains.annotations.NotNull;

public class Object {

    @Override
    public int hashCode() {
        return sandbox.java.lang.System.identityHashCode(this);
    }

    @Override
    @NotNull
    public java.lang.String toString() {
        return toDJVMString().toString();
    }

    @NotNull
    public String toDJVMString() {
        return toDJVMString(hashCode());
    }

    static String toDJVMString(int hashCode) {
        return String.toDJVM("sandbox.java.lang.Object@" + java.lang.Integer.toString(hashCode, 16));
    }

    @NotNull
    protected java.lang.Object fromDJVM() {
        return this;
    }

    public static java.lang.Object[] fromDJVM(java.lang.Object[] args) {
        if (args == null) {
            return null;
        }

        java.lang.Object[] unwrapped = (java.lang.Object[]) java.lang.reflect.Array.newInstance(
            fromDJVM(args.getClass().getComponentType()), args.length
        );
        int i = 0;
        for (java.lang.Object arg : args) {
            unwrapped[i] = unwrap(arg);
            ++i;
        }
        return unwrapped;
    }

    private static java.lang.Object unwrap(java.lang.Object arg) {
        if (arg instanceof Object) {
            return ((Object) arg).fromDJVM();
        } else if (arg != null && java.lang.Object[].class.isAssignableFrom(arg.getClass())) {
            return fromDJVM((java.lang.Object[]) arg);
        } else {
            return arg;
        }
    }

    @NotNull
    private static Class<?> fromDJVM(Class<?> type) {
        try {
            return DJVM.fromDJVMType(type);
        } catch (ClassNotFoundException e) {
            throw DJVM.fail(e.getMessage());
        }
    }
}
