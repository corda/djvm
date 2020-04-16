package sandbox.java.lang.reflect;

import org.jetbrains.annotations.NotNull;
import sandbox.java.lang.String;
import sandbox.java.lang.annotation.Annotation;

@SuppressWarnings("unused")
public abstract class Executable extends AccessibleObject implements Member, GenericDeclaration {
    Executable() {}

    abstract java.lang.reflect.Executable getRoot();

    @Override
    public abstract Class<?> getDeclaringClass();

    @Override
    public abstract String getName();

    @Override
    public abstract int getModifiers();

    @Override
    public abstract boolean isSynthetic();

    public abstract boolean isVarArgs();

    @Override
    public abstract TypeVariable<?>[] getTypeParameters();

    public abstract Class<?>[] getParameterTypes();

    @NotNull
    public Parameter[] getParameters() {
        java.lang.reflect.Parameter[] source = getRoot().getParameters();
        Parameter[] parameters = new Parameter[source.length];
        for (int i = 0; i < source.length; ++i) {
            parameters[i] = new Parameter(source[i], this);
        }
        return parameters;
    }

    public int getParameterCount() {
        throw new AbstractMethodError();
    }

    public abstract Type[] getGenericParameterTypes();

    public abstract Class<?>[] getExceptionTypes();

    public abstract Type[] getGenericExceptionTypes();

    public abstract String toGenericString();

    public abstract Annotation[][] getParameterAnnotations();

    public abstract AnnotatedType getAnnotatedReturnType();

    public AnnotatedType getAnnotatedReceiverType() {
        throw sandbox.java.lang.DJVM.fail(FORBIDDEN_METHOD + getClass().getName() + ".getAnnotatedReceiverType()");
    }

    public AnnotatedType[] getAnnotatedParameterTypes() {
        throw sandbox.java.lang.DJVM.fail(FORBIDDEN_METHOD + getClass().getName() + ".getAnnotatedParameterTypes()");
    }

    public AnnotatedType[] getAnnotatedExceptionTypes() {
        throw sandbox.java.lang.DJVM.fail(FORBIDDEN_METHOD + getClass().getName() + ".getAnnotatedExceptionTypes()");
    }
}
