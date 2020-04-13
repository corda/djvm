package sandbox.java.lang.reflect;

import org.jetbrains.annotations.NotNull;
import sandbox.java.lang.String;
import sandbox.java.lang.annotation.Annotation;

@SuppressWarnings("unused")
public final class Field extends AccessibleObject implements Member {
    private static final java.lang.String FORBIDDEN_METHOD = "Disallowed reference to API; java.lang.reflect.Field.";

    private final java.lang.reflect.Field field;
    private final String name;
    private final String stringValue;
    private final String genericString;

    Field(@NotNull java.lang.reflect.Field field) {
        this.field = field;
        this.name = String.toDJVM(field.getName());
        this.stringValue = String.toDJVM(field.toString());
        this.genericString = String.toDJVM(field.toGenericString());
    }

    @Override
    public boolean equals(java.lang.Object other) {
        if (this == other) {
            return true;
        } else if (!(other instanceof Field)) {
            return false;
        } else {
            return field.equals(other);
        }
    }

    @Override
    public int hashCode() {
        return DJVM.hashCodeFor(field.hashCode());
    }

    @Override
    @NotNull
    public String toDJVMString() {
        return stringValue;
    }

    public String toGenericString() {
        return genericString;
    }

    @Override
    public Class<?> getDeclaringClass() {
        return field.getDeclaringClass();
    }

    public Class<?> getType() {
        return field.getType();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getModifiers() {
        return field.getModifiers();
    }

    public boolean isEnumConstant() {
        return field.isEnumConstant();
    }

    @Override
    public boolean isSynthetic() {
        return field.isSynthetic();
    }

    public Type getGenericType() {
        throw sandbox.java.lang.DJVM.fail(FORBIDDEN_METHOD + "getGenericType()");
    }

    public java.lang.Object get(java.lang.Object obj) {
        throw sandbox.java.lang.DJVM.fail(FORBIDDEN_METHOD + "get(Object)");
    }

    public void set(java.lang.Object obj, java.lang.Object value) {
        throw sandbox.java.lang.DJVM.fail(FORBIDDEN_METHOD + "set(Object, Object)");
    }

    public boolean getBoolean(java.lang.Object obj) {
        throw sandbox.java.lang.DJVM.fail(FORBIDDEN_METHOD + "getBoolean(Object)");
    }

    public void setBoolean(java.lang.Object obj, boolean z) {
        throw sandbox.java.lang.DJVM.fail(FORBIDDEN_METHOD + "setBoolean(Object, boolean)");
    }

    public byte getByte(java.lang.Object obj) {
        throw sandbox.java.lang.DJVM.fail(FORBIDDEN_METHOD + "getByte(Object)");
    }

    public void setByte(java.lang.Object obj, byte b) {
        throw sandbox.java.lang.DJVM.fail(FORBIDDEN_METHOD + "setByte(Object, byte)");
    }

    public char getChar(java.lang.Object obj) {
        throw sandbox.java.lang.DJVM.fail(FORBIDDEN_METHOD + "getChar(Object)");
    }

    public void setChar(java.lang.Object obj, char c) {
        throw sandbox.java.lang.DJVM.fail(FORBIDDEN_METHOD + "setChar(Object, char)");
    }

    public short getShort(java.lang.Object obj) {
        throw sandbox.java.lang.DJVM.fail(FORBIDDEN_METHOD + "getShort(Object)");
    }

    public void setShort(java.lang.Object obj, short s) {
        throw sandbox.java.lang.DJVM.fail(FORBIDDEN_METHOD + "setShort(Object, short)");
    }

    public int getInt(java.lang.Object obj) {
        throw sandbox.java.lang.DJVM.fail(FORBIDDEN_METHOD + "getInt(Object)");
    }

    public void setInt(java.lang.Object obj, int i) {
        throw sandbox.java.lang.DJVM.fail(FORBIDDEN_METHOD + "setInt(Object, int)");
    }

    public long getLong(java.lang.Object obj) {
        throw sandbox.java.lang.DJVM.fail(FORBIDDEN_METHOD + "getLong(Object)");
    }

    public void setLong(java.lang.Object obj, long l) {
        throw sandbox.java.lang.DJVM.fail(FORBIDDEN_METHOD + "setLong(Object, long)");
    }

    public float getFloat(java.lang.Object obj) {
        throw sandbox.java.lang.DJVM.fail(FORBIDDEN_METHOD + "getFloat(Object)");
    }

    public void setFloat(java.lang.Object obj, float f) {
        throw sandbox.java.lang.DJVM.fail(FORBIDDEN_METHOD + "setFloat(Object, float)");
    }

    public double getDouble(java.lang.Object obj) {
        throw sandbox.java.lang.DJVM.fail(FORBIDDEN_METHOD + "getDouble(Object)");
    }

    public void setDouble(java.lang.Object obj, double d) {
        throw sandbox.java.lang.DJVM.fail(FORBIDDEN_METHOD + "setDouble(Object, double)");
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        return sandbox.java.lang.DJVM.isAnnotationPresent(field, annotationType);
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return sandbox.java.lang.DJVM.getAnnotation(field, annotationClass);
    }

    @NotNull
    @Override
    public Annotation[] getAnnotations() {
        return sandbox.java.lang.DJVM.getAnnotations(field);
    }

    @NotNull
    @Override
    public Annotation[] getDeclaredAnnotations() {
        return sandbox.java.lang.DJVM.getDeclaredAnnotations(field);
    }

    @Override
    public <T extends Annotation> T getDeclaredAnnotation(Class<T> annotationClass) {
        return sandbox.java.lang.DJVM.getDeclaredAnnotation(field, annotationClass);
    }

    @NotNull
    @Override
    public <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass) {
        return sandbox.java.lang.DJVM.getAnnotationsByType(field, annotationClass);
    }

    @NotNull
    @Override
    public <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> annotationType) {
        return sandbox.java.lang.DJVM.getDeclaredAnnotationsByType(field, annotationType);
    }

    @Override
    @NotNull
    protected java.lang.reflect.Field fromDJVM() {
        return field;
    }
}
