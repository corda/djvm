package net.corda.djvm.execution;

import net.corda.djvm.JavaAnnotation;
import net.corda.djvm.JavaLabel;
import net.corda.djvm.JavaLabels;
import net.corda.djvm.JavaNestedAnnotations;
import net.corda.djvm.JavaAnnotationWithField;
import net.corda.djvm.TestBase;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.partitioningBy;
import static net.corda.djvm.AnnotationUtils.getNamesOf;
import static net.corda.djvm.AnnotationUtils.toStrings;
import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class AnnotatedJavaClassTest extends TestBase {
    AnnotatedJavaClassTest() {
        super(JAVA);
    }

    @Test
    void testSandboxAnnotation() {
        assertThat(UserJavaData.class.getAnnotation(JavaAnnotation.class)).isNotNull();

        sandbox(ctx -> {
            try {
                Class<?> sandboxClass = toSandboxClass(ctx, UserJavaData.class);
                @SuppressWarnings("unchecked")
                Class<? extends Annotation> sandboxAnnotation
                    = (Class<? extends Annotation>) toSandboxClass(ctx, "sandbox.net.corda.djvm.JavaAnnotation$1DJVM");
                Annotation annotationValue = sandboxClass.getAnnotation(sandboxAnnotation);
                assertThat(annotationValue).isNotNull();
                assertThat(annotationValue.toString())
                    .matches("^\\Q@sandbox.net.corda.djvm.JavaAnnotation$1DJVM(\\E(value=)?\"?Hello Java!\"?\\)$");
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testSandboxInheritingAnnotations() {
        assertThat(InheritingJavaData.class.getDeclaredAnnotations()).isNotEmpty();
        assertThat(InheritingJavaData.class.getAnnotations()).isNotEmpty();

        sandbox(ctx -> {
            try {
                Class<?> sandboxClass = toSandboxClass(ctx, InheritingJavaData.class);
                List<String> declaredAnnotations = toStrings(sandboxClass.getDeclaredAnnotations()).collect(toList());
                Stream<String> annotations = toStrings(sandboxClass.getAnnotations());
                assertThat(annotations)
                    .hasSizeGreaterThan(declaredAnnotations.size())
                    .containsAll(declaredAnnotations);
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSandboxAnnotationWithEnumValue() {
        sandbox(ctx -> {
            try {
                Class<? extends Annotation> sandboxClass
                    = (Class<? extends Annotation>) toSandboxClass(ctx, "sandbox.net.corda.djvm.JavaAnnotation");
                Class<? extends Annotation> sandboxAnnotation
                    = (Class<? extends Annotation>) toSandboxClass(ctx, "sandbox.java.lang.annotation.Retention$1DJVM");
                Annotation annotationValue = sandboxClass.getAnnotation(sandboxAnnotation);
                assertThat(annotationValue).isNotNull();
                assertThat(annotationValue.toString())
                    .matches("^\\Q@sandbox.java.lang.annotation.Retention$1DJVM(\\E(value=)?\"?RUNTIME\"?\\)$");
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSandboxAnnotationWithEnumArrayValue() {
        sandbox(ctx -> {
            try {
                Class<? extends Annotation> sandboxClass
                    = (Class<? extends Annotation>) toSandboxClass(ctx, "sandbox.net.corda.djvm.JavaAnnotation");
                Class<? extends Annotation> sandboxAnnotation
                    = (Class<? extends Annotation>) toSandboxClass(ctx, "sandbox.java.lang.annotation.Target$1DJVM");
                Method valueMethod = sandboxAnnotation.getMethod("value");
                Annotation annotationValue = sandboxClass.getAnnotation(sandboxAnnotation);
                assertThat(annotationValue).isNotNull();
                String[] policy = (String[]) valueMethod.invoke(annotationValue);
                assertThat(policy)
                    .containsExactlyInAnyOrder("TYPE", "CONSTRUCTOR", "METHOD", "FIELD", "PACKAGE");
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testMethodAnnotationOutsideSandbox() {
        sandbox(singleton(JavaAnnotation.class), ctx -> {
            try {
                Class<?> sandboxClass = toSandboxClass(ctx, UserJavaElements.class);
                Method sandboxMethod = sandboxClass.getDeclaredMethod("doNothing");
                Annotation[] annotations = sandboxMethod.getAnnotations();
                Stream<String> names = getNamesOf(annotations);
                assertThat(names).containsExactlyInAnyOrder(
                    "sandbox.net.corda.djvm.JavaAnnotation$1DJVM",
                    "net.corda.djvm.JavaAnnotation"
                );
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testFieldAnnotationOutsideSandbox() {
        sandbox(singleton(JavaAnnotation.class), ctx -> {
            try {
                Class<?> sandboxClass = toSandboxClass(ctx, UserJavaElements.class);
                Field sandboxField = sandboxClass.getDeclaredField("data");
                Annotation[] annotations = sandboxField.getAnnotations();
                Stream<String> names = getNamesOf(annotations);
                assertThat(names).containsExactlyInAnyOrder(
                    "sandbox.net.corda.djvm.JavaAnnotation$1DJVM",
                    "net.corda.djvm.JavaAnnotation"
                );
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testReflectionCanFetchAllSandboxedAnnotations() {
        sandbox(ctx -> {
            try {
                Class<?> sandboxClass = toSandboxClass(ctx, UserJavaData.class);
                Annotation[] annotations = sandboxClass.getAnnotations();
                Stream<String> names = getNamesOf(annotations);
                assertThat(names).containsExactlyInAnyOrder(
                    "sandbox.net.corda.djvm.JavaAnnotation$1DJVM"
                );
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testReflectionCanFetchAllStitchedAnnotations() {
        sandbox(singleton(JavaAnnotation.class), ctx -> {
            try {
                Class<?> sandboxClass = toSandboxClass(ctx, UserJavaData.class);
                Annotation[] annotations = sandboxClass.getAnnotations();
                Stream<String> names = getNamesOf(annotations);
                assertThat(names).containsExactlyInAnyOrder(
                    "sandbox.net.corda.djvm.JavaAnnotation$1DJVM",
                    "net.corda.djvm.JavaAnnotation"
                );
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testReflectionCanFetchAllMetaAnnotations() {
        sandbox(ctx -> {
            try {
                @SuppressWarnings("unchecked")
                Class<? extends Annotation> sandboxAnnotation
                    = (Class<? extends Annotation>) toSandboxClass(ctx, JavaAnnotation.class);
                Annotation[] annotations = sandboxAnnotation.getAnnotations();
                Stream<String> names = getNamesOf(annotations);
                assertThat(names).containsExactlyInAnyOrder(
                    "sandbox.java.lang.annotation.Retention$1DJVM",
                    "sandbox.java.lang.annotation.Target$1DJVM",
                    "java.lang.annotation.Documented",
                    "java.lang.annotation.Inherited"
                );
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testFunctionalInterfaceIsPreserved() {
        sandbox(ctx -> {
            try {
                Class<?> sandboxFunction = toSandboxClass(ctx, Function.class);
                Annotation[] sandboxAnnotations = sandboxFunction.getAnnotations();
                Stream<String> names = getNamesOf(sandboxAnnotations);
                assertThat(names).containsExactlyInAnyOrder(
                    "java.lang.FunctionalInterface"
                );
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testSingleRepeatableAnnotationFromOutsideSandbox() {
        sandbox(singleton(JavaLabel.class), ctx -> {
            try {
                Class<?> sandboxClass = toSandboxClass(ctx, UserJavaLabel.class);
                Annotation[] annotations = sandboxClass.getAnnotations();
                Map<Boolean, List<Annotation>> annotationMapping = Arrays.stream(annotations)
                    .collect(partitioningBy(AnnotatedJavaClassTest::isForSandbox, toList()));

                List<Annotation> sandboxAnnotations = annotationMapping.get(true);
                assertEquals(1, sandboxAnnotations.size());
                Annotation sandboxAnnotation = sandboxAnnotations.get(0);
                assertThat(sandboxAnnotation.toString())
                    .matches("^\\Q@sandbox.net.corda.djvm.JavaLabel$1DJVM(name=\\E\"?ZERO\"?\\)$");

                List<Annotation> javaAnnotations = annotationMapping.get(false);
                assertEquals(1, javaAnnotations.size());
                Annotation javaAnnotation = javaAnnotations.get(0);
                assertThat(javaAnnotation.toString())
                    .matches("^\\Q@net.corda.djvm.JavaLabel(name=\\E\"?ZERO\"?\\)$");
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testRepeatableAnnotationsFromOutsideSandbox() {
        sandbox(singleton(JavaLabels.class), ctx -> {
            try {
                Class<?> sandboxClass = toSandboxClass(ctx, UserJavaLabels.class);
                Annotation[] annotations = sandboxClass.getAnnotations();
                Map<Boolean, List<Annotation>> annotationMapping = Arrays.stream(annotations)
                    .collect(partitioningBy(AnnotatedJavaClassTest::isForSandbox, toList()));

                List<Annotation> sandboxAnnotations = annotationMapping.get(true);
                assertEquals(1, sandboxAnnotations.size());
                Annotation sandboxAnnotation = sandboxAnnotations.get(0);
                assertThat(sandboxAnnotation.toString())
                    .startsWith("@sandbox.net.corda.djvm.JavaLabels$1DJVM(")
                    .containsPattern("\\Q@sandbox.net.corda.djvm.JavaLabel$1DJVM(name=\\E\"?ONE\"?\\)")
                    .containsPattern("\\Q@sandbox.net.corda.djvm.JavaLabel$1DJVM(name=\\E\"?TWO\"?\\)")
                    .containsPattern("\\Q@sandbox.net.corda.djvm.JavaLabel$1DJVM(name=\\E\"?FIVE\"?\\)")
                    .endsWith(")");

                List<Annotation> javaAnnotations = annotationMapping.get(false);
                assertEquals(1, javaAnnotations.size());
                Annotation javaAnnotation = javaAnnotations.get(0);
                assertThat(javaAnnotation.toString())
                    .startsWith("@net.corda.djvm.JavaLabels(")
                    .containsPattern("\\Q@net.corda.djvm.JavaLabel(name=\\E\"?ONE\"?\\)")
                    .containsPattern("\\Q@net.corda.djvm.JavaLabel(name=\\E\"?TWO\"?\\)")
                    .containsPattern("\\Q@net.corda.djvm.JavaLabel(name=\\E\"?FIVE\"?\\)")
                    .endsWith(")");
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testReflectionCanFetchRepeatable() {
        sandbox(ctx -> {
            try {
                @SuppressWarnings("unchecked")
                Class<? extends Annotation> sandboxAnnotation
                    = (Class<? extends Annotation>) toSandboxClass(ctx, JavaLabel.class);
                Annotation[] annotations = sandboxAnnotation.getAnnotations();
                Stream<String> names = getNamesOf(annotations);
                assertThat(names).containsExactlyInAnyOrder(
                    "sandbox.java.lang.annotation.Repeatable$1DJVM",
                    "sandbox.java.lang.annotation.Retention$1DJVM",
                    "sandbox.java.lang.annotation.Target$1DJVM",
                    "java.lang.annotation.Documented"
                );
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testNestedAnnotationsFromOutsideSandbox() {
        sandbox(ctx -> {
            try {
                @SuppressWarnings("unchecked")
                Class<? extends Annotation> sandboxClass
                    = (Class<? extends Annotation>) toSandboxClass(ctx, UserNestedAnnotations.class);
                Annotation[] annotations = sandboxClass.getAnnotations();
                Stream<String> names = getNamesOf(annotations);
                assertThat(names).containsExactly(
                    "sandbox.net.corda.djvm.JavaNestedAnnotations$1DJVM"
                );
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testAnnotationWithField() {
        sandbox(ctx -> {
            try {
                Class<?> sandboxClass = toSandboxClass(ctx, UserJavaAnnotationFieldData.class);
                @SuppressWarnings("unchecked")
                Class<? extends Annotation> sandboxAnnotation
                    = (Class<? extends Annotation>) toSandboxClass(ctx, "sandbox.net.corda.djvm.JavaAnnotationWithField$1DJVM");
                Annotation annotationValue = sandboxClass.getAnnotation(sandboxAnnotation);
                assertThat(annotationValue).isNotNull();
                assertThat(annotationValue.toString())
                    .isEqualTo("@sandbox.net.corda.djvm.JavaAnnotationWithField$1DJVM()");
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    static boolean isForSandbox(@NotNull Annotation annotation) {
        return annotation.annotationType().getName().startsWith("sandbox.");
    }

    @SuppressWarnings("WeakerAccess")
    @JavaAnnotation("Hello Java!")
    static class UserJavaData {}

    @SuppressWarnings("unused")
    static class UserJavaElements {
        @JavaAnnotation("Hello Java Field!")
        String data;

        @JavaAnnotation("Hello Java Method!")
        void doNothing() {}
    }

    @SuppressWarnings("WeakerAccess")
    @JavaLabel(name = "ZERO")
    static class UserJavaLabel {}

    @SuppressWarnings("WeakerAccess")
    @JavaLabel(name = "ONE")
    @JavaLabel(name = "TWO")
    @JavaLabel(name = "FIVE")
    static class UserJavaLabels {}

    @SuppressWarnings("WeakerAccess")
    @JavaNestedAnnotations(
        annotationData = @JavaAnnotation("Nested Annotation")
    )
    static class UserNestedAnnotations {}

    @JavaAnnotation("Inherited")
    @JavaLabel(name = "Parent")
    static class BaseJavaData {}

    @SuppressWarnings("WeakerAccess")
    @JavaLabel(name = "Child")
    static class InheritingJavaData extends BaseJavaData {}

    @SuppressWarnings("WeakerAccess")
    @JavaAnnotationWithField
    static class UserJavaAnnotationFieldData {}
}
