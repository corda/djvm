package sandbox.java.lang.reflect;

import sandbox.java.lang.annotation.Annotation;

@SuppressWarnings("unused")
public class AccessibleObject extends sandbox.java.lang.Object implements AnnotatedElement {
    private static final String ZOMBIE_METHOD = "Zombie method invoked!";

    String named(String methodName) {
        return sandbox.java.lang.DJVM.unsandbox(getClass().getName()) + '.' + methodName;
    }

    public static void setAccessible(AccessibleObject[] array, boolean flag) {
        throw sandbox.java.lang.DJVM.failApi(
            "java.lang.reflect.AccessibleObject.setAccessible(AccessibleObject[], boolean)"
        );
    }

    AccessibleObject() {}

    public boolean isAccessible() {
        return false;
    }

    public void setAccessible(boolean flag) {
        throw sandbox.java.lang.DJVM.failApi(named("setAccessible(boolean)"));
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
