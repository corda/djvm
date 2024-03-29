package sandbox.java.lang.reflect;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sandbox.java.lang.String;
import sandbox.java.lang.annotation.Annotation;
import static sandbox.java.lang.DJVM.intern;

@SuppressWarnings("unused")
public final class Method extends Executable {
    private static final java.lang.String STRING_VALUE = "public sandbox.java.lang.String sandbox.java.lang.Object.toString()";
    private static final java.lang.String TO_STRING = "toString";

    private final java.lang.reflect.Method method;
    private final String name;
    private final String stringValue;
    private final String genericString;

    Method(@NotNull java.lang.reflect.Method method) {
        this.method = method;
        if (isToDJVMString(method)) {
            name = intern(TO_STRING);
            genericString = stringValue = intern(STRING_VALUE);
        } else {
            name = String.toDJVM(method.getName());
            stringValue = String.toDJVM(method.toString());
            genericString = String.toDJVM(method.toGenericString());
        }
    }

    private static boolean isToDJVMString(@NotNull java.lang.reflect.Method method) {
        return "toDJVMString".equals(method.getName()) && method.getParameters().length == 0;
    }

    @Override
    java.lang.reflect.Executable getRoot() {
        return method;
    }

    @Override
    public boolean equals(java.lang.Object other) {
        if (this == other) {
            return true;
        } else if (!(other instanceof Method)) {
            return false;
        } else {
            return method.equals(other);
        }
    }

    @Override
    public int hashCode() {
        return DJVM.hashCodeFor(method.hashCode());
    }

    @Override
    @NotNull
    public String toDJVMString() {
        return stringValue;
    }

    @Override
    @NotNull
    public Class<?> getDeclaringClass() {
        return method.getDeclaringClass();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toGenericString() {
        return genericString;
    }

    @Override
    public int getModifiers() {
        return method.getModifiers();
    }

    @Override
    public boolean isSynthetic() {
        return method.isSynthetic();
    }

    public boolean isDefault() {
        return method.isDefault();
    }

    public boolean isBridge() {
        return method.isBridge();
    }

    @Override
    public boolean isVarArgs() {
        return method.isVarArgs();
    }

    @NotNull
    public Class<?> getReturnType() {
        return method.getReturnType();
    }

    @Override
    @NotNull
    public Class<?>[] getParameterTypes() {
        return method.getParameterTypes();
    }

    @Override
    public int getParameterCount() {
        return method.getParameterCount();
    }

    @Override
    @NotNull
    public Annotation[][] getParameterAnnotations() {
        return sandbox.java.lang.DJVM.getParameterAnnotations(method);
    }

    @Override
    @NotNull
    public Class<?>[] getExceptionTypes() {
        return method.getExceptionTypes();
    }

    @Nullable
    public java.lang.Object getDefaultValue() {
        return sandbox.java.lang.DJVM.getDefaultValue(method);
    }

    @Override
    public Type[] getGenericParameterTypes() {
        throw sandbox.java.lang.DJVM.failApi(named("getGenericParameterTypes()"));
    }

    public Type getGenericReturnType() {
        throw sandbox.java.lang.DJVM.failApi(named("getGenericReturnType()"));
    }

    @Override
    public TypeVariable<?>[] getTypeParameters() {
        throw sandbox.java.lang.DJVM.failApi(named("getTypeParameters()"));
    }

    @Override
    public Type[] getGenericExceptionTypes() {
        throw sandbox.java.lang.DJVM.failApi(named("getGenericExceptionTypes()"));
    }

    @Override
    public AnnotatedType getAnnotatedReturnType() {
        throw sandbox.java.lang.DJVM.failApi(named("getAnnotatedReturnValue()"));
    }

    public java.lang.Object invoke(java.lang.Object obj, java.lang.Object... args) {
        throw sandbox.java.lang.DJVM.failApi(named("invoke(Object, Object...)"));
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        return sandbox.java.lang.DJVM.isAnnotationPresent(method, annotationType);
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
        return sandbox.java.lang.DJVM.getAnnotation(method, annotationType);
    }

    @Override
    @NotNull
    public Annotation[] getAnnotations() {
        return sandbox.java.lang.DJVM.getAnnotations(method);
    }

    @Override
    @NotNull
    public Annotation[] getDeclaredAnnotations() {
        return sandbox.java.lang.DJVM.getDeclaredAnnotations(method);
    }

    @Override
    public <T extends Annotation> T getDeclaredAnnotation(Class<T> annotationType) {
        return sandbox.java.lang.DJVM.getDeclaredAnnotation(method, annotationType);
    }

    @Override
    @NotNull
    public <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationType) {
        return sandbox.java.lang.DJVM.getAnnotationsByType(method, annotationType);
    }

    @Override
    @NotNull
    public <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> annotationType) {
        return sandbox.java.lang.DJVM.getDeclaredAnnotationsByType(method, annotationType);
    }

    @Override
    @NotNull
    protected java.lang.reflect.Method fromDJVM() {
        return method;
    }
}
