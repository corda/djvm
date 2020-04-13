package sandbox.java.lang.reflect;

import org.jetbrains.annotations.NotNull;
import sandbox.java.lang.String;
import sandbox.java.lang.annotation.Annotation;

@SuppressWarnings("unused")
public final class Method extends Executable {
    private static final java.lang.String FORBIDDEN_METHOD = "Disallowed reference to API; java.lang.reflect.Method.";

    private final java.lang.reflect.Method method;
    private final String name;
    private final String stringValue;
    private final String genericString;

    Method(@NotNull java.lang.reflect.Method method) {
        this.method = method;
        this.name = String.toDJVM(method.getName());
        this.stringValue = String.toDJVM(method.toString());
        this.genericString = String.toDJVM(method.toGenericString());
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

    public Class<?> getReturnType() {
        return method.getReturnType();
    }

    @Override
    public Class<?>[] getParameterTypes() {
        return method.getParameterTypes();
    }

    @NotNull
    @Override
    public Parameter[] getParameters() {
        java.lang.reflect.Parameter[] source = method.getParameters();
        Parameter[] parameters = new Parameter[source.length];
        for (int i = 0; i < source.length; ++i) {
            parameters[i] = new Parameter(source[i], this);
        }
        return parameters;
    }

    @Override
    public int getParameterCount() {
        return method.getParameterCount();
    }

    @Override
    public Class<?>[] getExceptionTypes() {
        return method.getExceptionTypes();
    }

    public java.lang.Object getDefaultValue() {
        return sandbox.java.lang.DJVM.getDefaultValue(method);
    }

    @Override
    public Type[] getGenericParameterTypes() {
        throw sandbox.java.lang.DJVM.fail(FORBIDDEN_METHOD + "getGenericParameterTypes()");
    }

    public Type getGenericReturnType() {
        throw sandbox.java.lang.DJVM.fail(FORBIDDEN_METHOD + "getGenericReturnType()");
    }

    @Override
    public TypeVariable<?>[] getTypeParameters() {
        throw sandbox.java.lang.DJVM.fail(FORBIDDEN_METHOD + "getTypeParameters()");
    }

    @Override
    public Type[] getGenericExceptionTypes() {
        throw sandbox.java.lang.DJVM.fail(FORBIDDEN_METHOD + "getGenericExceptionTypes()");
    }

    @Override
    public Annotation[][] getParameterAnnotations() {
        throw sandbox.java.lang.DJVM.fail(FORBIDDEN_METHOD + "getParameterAnnotations()");
    }

    @Override
    public AnnotatedType getAnnotatedReturnType() {
        throw sandbox.java.lang.DJVM.fail(FORBIDDEN_METHOD + "getAnnotatedReturnValue()");
    }

    public java.lang.Object invoke(java.lang.Object obj, java.lang.Object... args) {
        throw sandbox.java.lang.DJVM.fail(FORBIDDEN_METHOD + "invoke(Object, Object...)");
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        return sandbox.java.lang.DJVM.isAnnotationPresent(method, annotationType);
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
        return sandbox.java.lang.DJVM.getAnnotation(method, annotationType);
    }

    @NotNull
    @Override
    public Annotation[] getAnnotations() {
        return sandbox.java.lang.DJVM.getAnnotations(method);
    }

    @NotNull
    @Override
    public Annotation[] getDeclaredAnnotations() {
        return sandbox.java.lang.DJVM.getDeclaredAnnotations(method);
    }

    @Override
    public <T extends Annotation> T getDeclaredAnnotation(Class<T> annotationType) {
        return sandbox.java.lang.DJVM.getDeclaredAnnotation(method, annotationType);
    }

    @NotNull
    @Override
    public <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationType) {
        return sandbox.java.lang.DJVM.getAnnotationsByType(method, annotationType);
    }

    @NotNull
    @Override
    public <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> annotationType) {
        return sandbox.java.lang.DJVM.getDeclaredAnnotationsByType(method, annotationType);
    }

    @Override
    @NotNull
    protected java.lang.reflect.Method fromDJVM() {
        return method;
    }
}
