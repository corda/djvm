package sandbox.java.lang.reflect;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import sandbox.java.lang.String;
import sandbox.java.lang.annotation.Annotation;

@SuppressWarnings("unused")
public final class Parameter extends sandbox.java.lang.Object implements AnnotatedElement {
    private static final java.lang.String FORBIDDEN_METHOD = "Disallowed reference to API; java.lang.reflect.Parameter.";

    private final java.lang.reflect.Parameter parameter;
    private final Executable executable;
    private final String name;
    private final String stringValue;

    Parameter(@NotNull java.lang.reflect.Parameter parameter, Executable executable) {
        this.parameter = parameter;
        this.executable = executable;
        this.name = String.toDJVM(parameter.getName());
        this.stringValue = String.toDJVM(parameter.toString());
    }

    @Override
    public boolean equals(java.lang.Object other) {
        if (this == other) {
            return true;
        } else if (!(other instanceof Parameter)) {
            return false;
        } else {
            return parameter.equals(other);
        }
    }

    @Override
    public int hashCode() {
        return DJVM.hashCodeFor(parameter.hashCode());
    }

    @Override
    @NotNull
    public String toDJVMString() {
        return stringValue;
    }

    public Executable getDeclaringExecutable() {
        return executable;
    }

    public String getName() {
        return name;
    }

    public int getModifiers() {
        return parameter.getModifiers();
    }

    public boolean isNamePresent() {
        return parameter.isNamePresent();
    }

    public boolean isImplicit() {
        return parameter.isImplicit();
    }

    public boolean isSynthetic() {
        return parameter.isSynthetic();
    }

    public boolean isVarArgs() {
        return parameter.isVarArgs();
    }

    public Class<?> getType() {
        return parameter.getType();
    }

    public Type getParameterizedType() {
        throw sandbox.java.lang.DJVM.fail(FORBIDDEN_METHOD + "getParameterizedType()");
    }

    @Contract(" -> fail")
    public AnnotatedType getAnnotatedType() {
        throw sandbox.java.lang.DJVM.fail(FORBIDDEN_METHOD + "getAnnotatedType()");
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        return sandbox.java.lang.DJVM.isAnnotationPresent(parameter, annotationType);
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
        return sandbox.java.lang.DJVM.getAnnotation(parameter, annotationType);
    }

    @NotNull
    @Override
    public Annotation[] getAnnotations() {
        return sandbox.java.lang.DJVM.getAnnotations(parameter);
    }

    @NotNull
    @Override
    public Annotation[] getDeclaredAnnotations() {
        return sandbox.java.lang.DJVM.getDeclaredAnnotations(parameter);
    }

    @Override
    public <T extends Annotation> T getDeclaredAnnotation(Class<T> annotationType) {
        return sandbox.java.lang.DJVM.getDeclaredAnnotation(parameter, annotationType);
    }

    @NotNull
    @Override
    public <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationType) {
        return sandbox.java.lang.DJVM.getAnnotationsByType(parameter, annotationType);
    }

    @NotNull
    @Override
    public <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> annotationType) {
        return sandbox.java.lang.DJVM.getDeclaredAnnotationsByType(parameter, annotationType);
    }

    @Override
    @NotNull
    protected java.lang.reflect.Parameter fromDJVM() {
        return parameter;
    }
}
