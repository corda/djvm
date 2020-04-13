package sandbox.java.lang.reflect;

import sandbox.java.lang.String;
import sandbox.java.lang.annotation.Annotation;

@SuppressWarnings("unused")
public abstract class Executable extends AccessibleObject implements Member, GenericDeclaration {
    Executable() {}

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

    public abstract Parameter[] getParameters();

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
        throw sandbox.java.lang.DJVM.fail(ZOMBIE_METHOD);
    }
}
