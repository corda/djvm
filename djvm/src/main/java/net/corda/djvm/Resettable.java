package net.corda.djvm;

import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;

import static java.security.AccessController.doPrivileged;

final class Resettable {
    private static final Unsafe unsafe;

    static {
        try {
            unsafe = (Unsafe) doPrivileged((PrivilegedExceptionAction<?>)() -> {
                Field f = Unsafe.class.getDeclaredField("theUnsafe");
                f.setAccessible(true);
                return f.get(null);
            });
        } catch(PrivilegedActionException e) {
            Throwable cause = e.getCause();
            throw new InternalError(cause.getMessage(), cause);
        }
    }

    private final Class<?> resetClass;
    private final MethodHandle resetMethod;
    private final Map<String, FieldData> fields;

    Resettable(Class<?> resetClass, MethodHandle resetMethod) {
        this.resetClass = resetClass;
        this.resetMethod = resetMethod;
        this.fields = new HashMap<>();
    }

    MethodHandle getResetMethod() {
        return resetMethod;
    }

    void reset(Object value, String fieldName) {
        fields.computeIfAbsent(fieldName, this::createFieldData).setValue(value);
    }

    private FieldData createFieldData(String fieldName) {
        return doPrivileged((PrivilegedAction<FieldData>)() -> {
            try {
                final Field field = resetClass.getDeclaredField(fieldName);
                return new FieldData(unsafe.staticFieldBase(field), unsafe.staticFieldOffset(field));
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        });
    }

    private static final class FieldData {
        private final Object base;
        private final long offset;

        FieldData(Object base, long offset ) {
            this.base = base;
            this.offset = offset;
        }

        void setValue(Object value) {
            unsafe.putObject(base, offset, value);
        }
    }
}
