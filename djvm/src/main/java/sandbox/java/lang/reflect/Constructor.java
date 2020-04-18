package sandbox.java.lang.reflect;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sandbox.java.lang.String;
import sandbox.java.lang.Throwable;
import sandbox.java.lang.annotation.Annotation;

@SuppressWarnings("unused")
public final class Constructor<T> extends Executable {
    private static final java.lang.String FORBIDDEN_METHOD = "Disallowed reference to API; java.lang.reflect.Constructor.";

    private final java.lang.reflect.Constructor<T> constructor;
    private final String name;
    private final String stringValue;
    private final String genericString;

    Constructor(@NotNull java.lang.reflect.Constructor<T> constructor) {
        this.constructor = constructor;
        this.name = String.toDJVM(constructor.getName());
        this.stringValue = String.toDJVM(constructor.toString());
        this.genericString = String.toDJVM(constructor.toGenericString());
    }

    @Override
    java.lang.reflect.Constructor getRoot() {
        return constructor;
    }

    @Override
    public boolean equals(java.lang.Object other) {
        if (this == other) {
            return true;
        } else if (!(other instanceof Constructor)) {
            return false;
        } else {
            return constructor.equals(other);
        }
    }

    @Override
    public int hashCode() {
        return DJVM.hashCodeFor(constructor.hashCode());
    }

    @Override
    @NotNull
    public String toDJVMString() {
        return stringValue;
    }

    @Override
    public Class<?> getDeclaringClass() {
        return constructor.getDeclaringClass();
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
        return constructor.getModifiers();
    }

    @Override
    public boolean isSynthetic() {
        return constructor.isSynthetic();
    }

    @Override
    public boolean isVarArgs() {
        return constructor.isVarArgs();
    }

    @Override
    public Class<?>[] getParameterTypes() {
        return constructor.getParameterTypes();
    }

    @Override
    public int getParameterCount() {
        return constructor.getParameterCount();
    }

    @Override
    @NotNull
    public Annotation[][] getParameterAnnotations() {
        return sandbox.java.lang.DJVM.getParameterAnnotations(constructor);
    }

    @Override
    public Class<?>[] getExceptionTypes() {
        return constructor.getExceptionTypes();
    }

    @Override
    public TypeVariable<Constructor<T>>[] getTypeParameters() {
        throw sandbox.java.lang.DJVM.fail(FORBIDDEN_METHOD + "getTypeParameters()");
    }

    @Override
    public Type[] getGenericParameterTypes() {
        throw sandbox.java.lang.DJVM.fail(FORBIDDEN_METHOD + "getGenericParameterTypes()");
    }

    @Override
    public Type[] getGenericExceptionTypes() {
        throw sandbox.java.lang.DJVM.fail(FORBIDDEN_METHOD + "getGenericExceptionTypes()");
    }

    @Override
    public AnnotatedType getAnnotatedReturnType() {
        throw sandbox.java.lang.DJVM.fail(FORBIDDEN_METHOD + "getAnnotatedReturnType()");
    }

    @Override
    public AnnotatedType getAnnotatedReceiverType() {
        throw sandbox.java.lang.DJVM.fail(FORBIDDEN_METHOD + "getAnnotatedReceiverType()");
    }

    @NotNull
    public T newInstance(java.lang.Object ... args) throws java.lang.Throwable {
        try {
            return constructor.newInstance(args);
        } catch (Exception e) {
            Throwable t = sandbox.java.lang.DJVM.doCatch(e);
            throw sandbox.java.lang.DJVM.fromDJVM(t);
        }
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        return sandbox.java.lang.DJVM.isAnnotationPresent(constructor, annotationType);
    }

    @Override
    @Nullable
    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        return sandbox.java.lang.DJVM.getAnnotation(constructor, annotationType);
    }

    @Override
    @NotNull
    public Annotation[] getAnnotations() {
        return sandbox.java.lang.DJVM.getAnnotations(constructor);
    }

    @Override
    @NotNull
    public Annotation[] getDeclaredAnnotations() {
        return sandbox.java.lang.DJVM.getDeclaredAnnotations(constructor);
    }

    @Override
    @Nullable
    public <A extends Annotation> A getDeclaredAnnotation(Class<A> annotationType) {
        return sandbox.java.lang.DJVM.getDeclaredAnnotation(constructor, annotationType);
    }

    @Override
    @NotNull
    public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType) {
        return sandbox.java.lang.DJVM.getAnnotationsByType(constructor, annotationType);
    }

    @Override
    @NotNull
    public <A extends Annotation> A[] getDeclaredAnnotationsByType(Class<A> annotationType) {
        return sandbox.java.lang.DJVM.getDeclaredAnnotationsByType(constructor, annotationType);
    }

    @Override
    @NotNull
    protected java.lang.reflect.Constructor<T> fromDJVM() {
        return constructor;
    }
}