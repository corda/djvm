package net.corda.djvm.execution;

import net.corda.djvm.JavaAnnotation;
import net.corda.djvm.JavaLabel;
import net.corda.djvm.JavaLabels;
import net.corda.djvm.JavaNestedAnnotations;
import net.corda.djvm.TestBase;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.partitioningBy;
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
                Class<?> sandboxClass = loadClass(ctx, UserJavaData.class.getName()).getType();
                @SuppressWarnings("unchecked")
                Class<? extends Annotation> sandboxAnnotation
                    = (Class<? extends Annotation>) loadClass(ctx, "sandbox.net.corda.djvm.JavaAnnotation$1DJVM").getType();
                Annotation annotationValue = sandboxClass.getAnnotation(sandboxAnnotation);
                assertThat(annotationValue).isNotNull();
                assertThat(annotationValue.toString())
                    .matches("^\\Q@sandbox.net.corda.djvm.JavaAnnotation$1DJVM(value=\\E\"?Hello Java!\"?\\)$");
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
                Class<?> sandboxClass = loadClass(ctx, InheritingJavaData.class.getName()).getType();
                List<String> declaredAnnotations = toStrings(sandboxClass.getDeclaredAnnotations());
                List<String> annotations = toStrings(sandboxClass.getAnnotations());
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
                    = (Class<? extends Annotation>) loadClass(ctx, "sandbox.net.corda.djvm.JavaAnnotation").getType();
                Class<? extends Annotation> sandboxAnnotation
                    = (Class<? extends Annotation>) loadClass(ctx, "sandbox.java.lang.annotation.Retention$1DJVM").getType();
                Annotation annotationValue = sandboxClass.getAnnotation(sandboxAnnotation);
                assertThat(annotationValue).isNotNull();
                assertThat(annotationValue.toString())
                    .matches("^\\Q@sandbox.java.lang.annotation.Retention$1DJVM(value=\\E\"?RUNTIME\"?\\)$");
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
                    = (Class<? extends Annotation>) loadClass(ctx, "sandbox.net.corda.djvm.JavaAnnotation").getType();
                Class<? extends Annotation> sandboxAnnotation
                    = (Class<? extends Annotation>) loadClass(ctx, "sandbox.java.lang.annotation.Target$1DJVM").getType();
                Method valueMethod = sandboxAnnotation.getMethod("value");
                Annotation annotationValue = sandboxClass.getAnnotation(sandboxAnnotation);
                assertThat(annotationValue).isNotNull();
                String[] policy = (String[]) valueMethod.invoke(annotationValue);
                assertThat(policy).containsExactlyInAnyOrder("TYPE", "METHOD", "FIELD");
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testMethodAnnotationOutsideSandbox() {
        sandbox(singleton(JavaAnnotation.class), ctx -> {
            try {
                Class<?> sandboxClass = loadClass(ctx, UserJavaData.class.getName()).getType();
                Method sandboxMethod = sandboxClass.getDeclaredMethod("doNothing");
                Annotation[] annotations = sandboxMethod.getAnnotations();
                List<String> names = getNamesOf(annotations);
                assertThat(names).containsExactlyInAnyOrder(
                    "sandbox.net.corda.djvm.JavaAnnotation$1DJVM", "net.corda.djvm.JavaAnnotation"
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
                Class<?> sandboxClass = loadClass(ctx, UserJavaData.class.getName()).getType();
                Annotation[] annotations = sandboxClass.getAnnotations();
                List<String> names = getNamesOf(annotations);
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
                Class<?> sandboxClass = loadClass(ctx, UserJavaData.class.getName()).getType();
                Annotation[] annotations = sandboxClass.getAnnotations();
                List<String> names = getNamesOf(annotations);
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
                    = (Class<? extends Annotation>) loadClass(ctx, JavaAnnotation.class.getName()).getType();
                Annotation[] annotations = sandboxAnnotation.getAnnotations();
                List<String> names = getNamesOf(annotations);
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
                Class<?> sandboxFunction = loadClass(ctx, Function.class.getName()).getType();
                Annotation[] sandboxAnnotations = sandboxFunction.getAnnotations();
                List<String> names = getNamesOf(sandboxAnnotations);
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
            Class<?> sandboxClass = loadClass(ctx, UserJavaLabel.class.getName()).getType();
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
        });
    }

    @Test
    void testRepeatableAnnotationsFromOutsideSandbox() {
        sandbox(singleton(JavaLabels.class), ctx -> {
            try {
                Class<?> sandboxClass = loadClass(ctx, UserJavaLabels.class.getName()).getType();
                Annotation[] annotations = sandboxClass.getAnnotations();
                Map<Boolean, List<Annotation>> annotationMapping = Arrays.stream(annotations)
                    .collect(partitioningBy(AnnotatedJavaClassTest::isForSandbox, toList()));

                List<Annotation> sandboxAnnotations = annotationMapping.get(true);
                assertEquals(1, sandboxAnnotations.size());
                Annotation sandboxAnnotation = sandboxAnnotations.get(0);
                assertThat(sandboxAnnotation.toString())
                    .startsWith("@sandbox.net.corda.djvm.JavaLabels$1DJVM(value=")
                    .containsPattern("\\Q@sandbox.net.corda.djvm.JavaLabel$1DJVM(name=\\E\"?ONE\"?\\)")
                    .containsPattern("\\Q@sandbox.net.corda.djvm.JavaLabel$1DJVM(name=\\E\"?TWO\"?\\)")
                    .containsPattern("\\Q@sandbox.net.corda.djvm.JavaLabel$1DJVM(name=\\E\"?FIVE\"?\\)")
                    .endsWith(")");

                List<Annotation> javaAnnotations = annotationMapping.get(false);
                assertEquals(1, javaAnnotations.size());
                Annotation javaAnnotation = javaAnnotations.get(0);
                assertThat(javaAnnotation.toString())
                    .startsWith("@net.corda.djvm.JavaLabels(value=")
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
                    = (Class<? extends Annotation>) loadClass(ctx, JavaLabel.class.getName()).getType();
                Annotation[] annotations = sandboxAnnotation.getAnnotations();
                List<String> names = getNamesOf(annotations);
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
                    = (Class<? extends Annotation>) loadClass(ctx, UserNestedAnnotations.class.getName()).getType();
                Annotation[] annotations = sandboxClass.getAnnotations();
                List<String> names = getNamesOf(annotations);
                assertThat(names).containsExactly(
                    "sandbox.net.corda.djvm.JavaNestedAnnotations$1DJVM"
                );
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    static boolean isForSandbox(@NotNull Annotation annotation) {
        return annotation.annotationType().getName().startsWith("sandbox.");
    }

    static List<String> getNamesOf(Annotation[] annotations) {
        return Arrays.stream(annotations)
            .map(ann -> ann.annotationType().getName())
            .collect(toList());
    }

    static List<String> toStrings(Annotation[] annotations) {
        return Arrays.stream(annotations)
            .map(Annotation::toString)
            .collect(toList());
    }

    @JavaAnnotation("Hello Java!")
    static class UserJavaData {
        @SuppressWarnings("unused")
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
}
