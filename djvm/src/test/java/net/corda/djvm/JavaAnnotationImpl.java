package net.corda.djvm;

@SuppressWarnings("ClassExplicitlyAnnotation")
public class JavaAnnotationImpl implements JavaAnnotation {
    private final String data;

    public JavaAnnotationImpl(String data) {
        this.data = data;
    }

    @Override
    public String value() {
        return data;
    }

    @Override
    public Class<? extends JavaAnnotation> annotationType() {
        return getClass();
    }

    @Override
    public String toString() {
        return "JavaAnnotationImpl[value='" + data + "\']";
    }
}
