package sandbox.java.lang.reflect;

import sandbox.java.lang.annotation.Annotation;

@SuppressWarnings("unused")
public class AccessibleObject extends sandbox.java.lang.Object implements AnnotatedElement {
    private static final java.lang.String FORBIDDEN_METHOD = "Disallowed reference to API; ";
    static final String ZOMBIE_METHOD = "Zombie method invoked!";

    AccessibleObject() {}

    public boolean isAccessible() {
        return false;
    }

    public static void setAccessible(AccessibleObject[] array, boolean flag) {
        throw sandbox.java.lang.DJVM.fail(
            FORBIDDEN_METHOD + "AccessibleObject.setAccessible(AccessibleObject[], boolean)"
        );
    }

    public void setAccessible(boolean flag) {
        throw sandbox.java.lang.DJVM.fail(FORBIDDEN_METHOD + getClass().getName() + ".setAccessible(boolean)");
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
        throw sandbox.java.lang.DJVM.fail(ZOMBIE_METHOD);
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        return AnnotatedElement.super.isAnnotationPresent(annotationType);
    }

    @Override
    public <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationType) {
        throw sandbox.java.lang.DJVM.fail(ZOMBIE_METHOD);
    }

    @Override
    public Annotation[] getAnnotations() {
        return getDeclaredAnnotations();
    }

    @Override
    public Annotation[] getDeclaredAnnotations()  {
        throw sandbox.java.lang.DJVM.fail(ZOMBIE_METHOD);
    }

    @Override
    public <T extends Annotation> T getDeclaredAnnotation(Class<T> annotationType) {
        return getAnnotation(annotationType);
    }

    @Override
    public <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> annotationType) {
        return getAnnotationsByType(annotationType);
    }
}
