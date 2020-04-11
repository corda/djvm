package net.corda.djvm.execution;

import net.corda.djvm.JavaAnnotation;
import net.corda.djvm.JavaAnnotationClassData;
import net.corda.djvm.JavaAnnotationData;
import net.corda.djvm.JavaAnnotationImpl;
import net.corda.djvm.JavaLabel;
import net.corda.djvm.JavaLabels;
import net.corda.djvm.JavaNestedAnnotations;
import net.corda.djvm.JavaUntrustworthy;
import net.corda.djvm.Label;
import net.corda.djvm.TestBase;
import net.corda.djvm.TypedTaskFactory;
import net.corda.djvm.WithJava;
import net.corda.djvm.rewiring.SandboxClassLoader;
import net.corda.djvm.rules.RuleViolationError;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.RetentionPolicy;
import java.util.*;
import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

class AnnotationProxyJavaTest extends TestBase {
    private static final String MESSAGE = "Hello, Sandbox!";
    private static final String STRING_DATA = "Annotation Data";
    private static final double DOUBLE_DATA = 99999.9999;
    private static final float FLOAT_DATA = 555.555f;
    private static final long LONG_DATA = 12345678L;
    private static final int INTEGER_DATA = 123456;
    private static final char CHAR_DATA = '\u03C0';
    private static final byte BYTE_DATA = 0x7f;

    AnnotationProxyJavaTest() {
        super(JAVA);
    }

    @Test
    void testAnnotationInsideSandbox() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String result = WithJava.run(taskFactory, ReadJavaAnnotation.class, null);
                assertThat(result)
                    .matches("^\\Q@sandbox.net.corda.djvm.JavaAnnotation(value=\\E\"?Hello Java!\"?\\)$");
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class ReadJavaAnnotation implements Function<String, String> {
        @Override
        public String apply(String unused) {
            JavaAnnotation value = UserJavaData.class.getAnnotation(JavaAnnotation.class);
            return value == null ? null : value.toString();
        }
    }

    @Test
    void testInheritingAnnotationsInsideSandbox() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String[][] result = WithJava.run(taskFactory, ReadInheritedAnnotations.class, null);
                assertThat(result).hasSize(2);
                assertThat(result[0]).containsExactlyInAnyOrder(
                    "@sandbox.net.corda.djvm.JavaLabel(name=Child)"
                );
                assertThat(result[1]).containsExactlyInAnyOrder(
                    "@sandbox.net.corda.djvm.JavaLabel(name=Child)",
                    "@sandbox.net.corda.djvm.JavaAnnotation(value=Inherited)"
                );
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class ReadInheritedAnnotations implements Function<String, String[][]> {
        private String[]toClassNames(Annotation[] annotations) {
            return Arrays.stream(annotations)
                .map(Annotation::toString)
                .toArray(String[]::new);
        }

        @Override
        public String[][] apply(String unused) {
            String[] declaredAnnotations = toClassNames(InheritingJavaData.class.getDeclaredAnnotations());
            String[] allAnnotations = toClassNames(InheritingJavaData.class.getAnnotations());
            return new String[][]{ declaredAnnotations, allAnnotations };
        }
    }

    @Test
    void testAnnotationOwnerIsValidInterface() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String result = WithJava.run(taskFactory, FakeProxyTask.class, MESSAGE);
                assertThat(result)
                    .isEqualTo("JavaAnnotationImpl[value='" + MESSAGE + "']");
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class FakeProxyTask implements Function<String, String> {
        @Override
        public String apply(String data) {
            return new JavaAnnotationImpl(data).toString();
        }
    }

    @Test
    void testAnnotationProxiesForEquality() {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                Function<Class<? extends Function<?, ?>>, ? extends Function<? super Object, ?>> taskFactory
                    = classLoader.createRawTaskFactory().compose(classLoader.createSandboxFunction());
                Object[] results = (Object[]) taskFactory.apply(GetAnnotationProxies.class).apply(null);
                assertThat(results).hasSize(2);
                assertThat(results[1])
                    .isEqualTo(results[0])
                    .isNotSameAs(results[0]);
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class GetAnnotationProxies implements Function<String, JavaAnnotation[]> {
        @Override
        public JavaAnnotation[] apply(String unused) {
            return new JavaAnnotation[]{
                Data1.class.getAnnotation(JavaAnnotation.class),
                Data2.class.getAnnotation(JavaAnnotation.class)
            };
        }
    }

    @Test
    void testEqualAnnotationsHaveEqualHashCodes() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                int[] hashCodes = WithJava.run(taskFactory, GetAnnotationHashCodes.class, null);
                assertThat(hashCodes).hasSize(2);
                assertThat(hashCodes[1])
                    .isEqualTo(hashCodes[0])
                    .isNotSameAs(hashCodes[0]);
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class GetAnnotationHashCodes implements Function<String, int[]> {
        @Override
        public int[] apply(String unused) {
            JavaAnnotation annotation1 = Data1.class.getAnnotation(JavaAnnotation.class);
            JavaAnnotation annotation2 = Data2.class.getAnnotation(JavaAnnotation.class);
            return (annotation1.equals(annotation2) && annotation2.equals(annotation1)) ? new int[]{
                annotation1.hashCode(),
                annotation2.hashCode()
            } : null;
        }
    }

    @Test
    void testComparingTrueProxyWithFakeProxy() {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                Function<Class<? extends Function<?, ?>>, ? extends Function<? super Object, ?>> taskFactory
                    = classLoader.createRawTaskFactory().compose(classLoader.createSandboxFunction());
                Object[] results = (Object[]) taskFactory.apply(GetTrueAndFakeAnnotations.class)
                    .apply(null);
                assertThat(results).hasSize(2);
                assertThat(results[1]).isNotEqualTo(results[0]);
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class GetTrueAndFakeAnnotations implements Function<String, JavaAnnotation[]> {
        @Override
        public JavaAnnotation[] apply(String unused) {
            return new JavaAnnotation[]{
                Data1.class.getAnnotation(JavaAnnotation.class),
                new JavaAnnotationImpl(MESSAGE)
            };
        }
    }

    @Test
    void testGetAnnotations() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String[] annotations = WithJava.run(taskFactory, GetAnnotations.class, null);
                assertThat(annotations).containsExactly(
                    "@sandbox.java.lang.annotation.Retention(value=RUNTIME)",
                    "@sandbox.java.lang.annotation.Target(value=[TYPE])",
                    "@sandbox.java.lang.annotation.Documented()",
                    "@sandbox.java.lang.annotation.Repeatable(value=interface sandbox.net.corda.djvm.JavaLabels)"
                );
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class GetAnnotations implements Function<String, String[]> {
        @Override
        public String[] apply(String unused) {
            Annotation[] annotations = JavaLabel.class.getAnnotations();
            return Arrays.stream(annotations)
                .map(Annotation::toString)
                .toArray(String[]::new);
        }
    }

    @Test
    void testGetAnnotationsByType() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String[] annotations = WithJava.run(taskFactory, GetAnnotationsByType.class, null);
                assertThat(annotations).containsExactly(
                    "@sandbox.net.corda.djvm.JavaLabel(name=HERE)",
                    "@sandbox.net.corda.djvm.JavaLabel(name=THERE)",
                    "@sandbox.net.corda.djvm.JavaLabel(name=EVERYWHERE)"
                );
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class GetAnnotationsByType implements Function<String, String[]> {
        @Override
        public String[] apply(String unused) {
            JavaLabel[] annotations = UserJavaLabels.class.getAnnotationsByType(JavaLabel.class);
            return Arrays.stream(annotations)
                .map(Annotation::toString)
                .toArray(String[]::new);
        }
    }

    @Test
    void testRepeatedAnnotationsToString() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String result = WithJava.run(taskFactory, GetRepeatedAnnotationsToString.class, null);
                assertThat(result).isEqualTo(
                    "@sandbox.net.corda.djvm.JavaLabels(value=" +
                        "[@sandbox.net.corda.djvm.JavaLabel(name=HERE)" +
                        ",@sandbox.net.corda.djvm.JavaLabel(name=THERE)" +
                        ",@sandbox.net.corda.djvm.JavaLabel(name=EVERYWHERE)]" +
                    ')'
                );
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class GetRepeatedAnnotationsToString implements Function<String, String> {
        @Override
        public String apply(String unused) {
            JavaLabels annotations = UserJavaLabels.class.getAnnotation(JavaLabels.class);
            return annotations == null ? null : annotations.toString();
        }
    }

    @Test
    void testEvilAnnotationsAreHandledSafely() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Throwable ex = assertThrows(RuleViolationError.class, () -> WithJava.run(taskFactory, EvilAnnotationTask.class, null));
                assertThat(ex)
                    .isExactlyInstanceOf(RuleViolationError.class)
                    .hasMessageStartingWith("java.lang.NoSuchMethodError -> ")
                    .hasMessageContaining("sandbox.java.lang.System.currentTimeMillis()")
                    .hasMessageMatching(".*( 'long sandbox\\..*\\(\\)'$|\\.currentTimeMillis\\(\\)J$)")
                    .hasNoCause();
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class EvilAnnotationTask implements Function<String, String> {
        @Override
        public String apply(String unused) {
            JavaUntrustworthy value = InnocentData.class.getAnnotation(JavaUntrustworthy.class);
            return value == null ? null : value.toString();
        }
    }

    @Test
    void testReadAnnotationSimpleData() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Object[] result = WithJava.run(taskFactory, ReadJavaAnnotationData.class, null);
                assertThat(result).containsExactly(
                    STRING_DATA,
                    LONG_DATA,
                    INTEGER_DATA,
                    DOUBLE_DATA,
                    FLOAT_DATA,
                    CHAR_DATA,
                    BYTE_DATA,
                    Label.GOOD,
                    true
                );
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class ReadJavaAnnotationData implements Function<String, Object[]> {
        @Override
        public Object[] apply(String unused) {
            JavaAnnotationData annotation = UserJavaData.class.getAnnotation(JavaAnnotationData.class);
            return annotation == null ? null : new Object[]{
                annotation.stringData(),
                annotation.longData(),
                annotation.intData(),
                annotation.doubleData(),
                annotation.floatData(),
                annotation.charData(),
                annotation.byteData(),
                annotation.label(),
                annotation.flag()
            };
        }
    }

    @Test
    void testReadAnnotationSimpleDefaultData() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Object[] result = WithJava.run(taskFactory, ReadJavaAnnotationDefaultData.class, null);
                assertThat(result).containsExactly("", 0L, 0, 0.0, 0.0f, '?', (byte)0, Label.UGLY, false);
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class ReadJavaAnnotationDefaultData implements Function<String, Object[]> {
        @Override
        public Object[] apply(String unused) {
            JavaAnnotationData annotation = UserJavaDefaultData.class.getAnnotation(JavaAnnotationData.class);
            return annotation == null ? null : new Object[]{
                annotation.stringData(),
                annotation.longData(),
                annotation.intData(),
                annotation.doubleData(),
                annotation.floatData(),
                annotation.charData(),
                annotation.byteData(),
                annotation.label(),
                annotation.flag()
            };
        }
    }

    @Test
    void testAnnotationWithClassData() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Object[] result = WithJava.run(taskFactory, ReadJavaAnnotationClassData.class, null);
                assertThat(result).containsExactly(
                    "sandbox.net.corda.djvm.JavaAnnotation",
                    "sandbox.java.lang.UnsupportedOperationException",
                    "sandbox.java.lang.annotation.RetentionPolicy",
                    "sandbox.java.util.LinkedHashSet",
                    "java.lang.Object"
                );
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class ReadJavaAnnotationClassData implements Function<String, String[]> {
        @Override
        public String[] apply(String unused) {
            JavaAnnotationClassData annotation = UserClassData.class.getAnnotation(JavaAnnotationClassData.class);
            return annotation == null ? null : new String[]{
                annotation.annotationClass().getName(),
                annotation.throwableClass().getName(),
                annotation.enumClass().getName(),
                annotation.collectionClass().getName(),
                annotation.anyOldClass().getName()
            };
        }
    }

    @Test
    void testNestedAnnotations() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Object[] result = WithJava.run(taskFactory, ReadNestedAnnotations.class, null);
                assertThat(result).containsExactly(
                    "Single", "ONE", "TWO", "THREE"
                );
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class ReadNestedAnnotations implements Function<String, String[]> {
        @Override
        public String[] apply(String unused) {
            JavaNestedAnnotations annotation = UserNestedAnnotations.class.getAnnotation(JavaNestedAnnotations.class);
            if (annotation == null) {
                return null;
            } else {
                List<String> data = new ArrayList<>();
                data.add(annotation.annotationData().value());
                Collections.addAll(data, Arrays.stream(annotation.annotationsData())
                    .map(JavaAnnotation::value).toArray(String[]::new));
                return data.toArray(new String[0]);
            }
        }
    }

    @SuppressWarnings("WeakerAccess")
    @JavaAnnotation(MESSAGE)
    static class Data1 {}

    @SuppressWarnings("WeakerAccess")
    @JavaAnnotation(MESSAGE)
    static class Data2 {}

    @SuppressWarnings("WeakerAccess")
    @JavaAnnotationData(
        stringData = STRING_DATA,
        longData = LONG_DATA,
        intData = INTEGER_DATA,
        doubleData = DOUBLE_DATA,
        floatData = FLOAT_DATA,
        charData = CHAR_DATA,
        byteData = BYTE_DATA,
        label = Label.GOOD,
        flag = true
    )
    @JavaAnnotation("Hello Java!")
    static class UserJavaData {}

    @SuppressWarnings("WeakerAccess")
    @JavaAnnotationData
    @JavaAnnotation
    static class UserJavaDefaultData {}

    @SuppressWarnings("WeakerAccess")
    @JavaUntrustworthy
    static class InnocentData {}

    @SuppressWarnings("WeakerAccess")
    @JavaAnnotationClassData(
        annotationClass = JavaAnnotation.class,
        throwableClass = UnsupportedOperationException.class,
        enumClass = RetentionPolicy.class,
        collectionClass = LinkedHashSet.class,
        anyOldClass = Object.class
    )
    static class UserClassData {}

    @SuppressWarnings("WeakerAccess")
    @JavaNestedAnnotations(
        annotationData = @JavaAnnotation("Single"),
        annotationsData = {
            @JavaAnnotation("ONE"),
            @JavaAnnotation("TWO"),
            @JavaAnnotation("THREE")
        }
    )
    static class UserNestedAnnotations {}

    @SuppressWarnings("WeakerAccess")
    @JavaLabel(name = "HERE")
    @JavaLabel(name = "THERE")
    @JavaLabel(name = "EVERYWHERE")
    static class UserJavaLabels {}

    @JavaAnnotation("Inherited")
    @JavaLabel(name = "Parent")
    static class BaseJavaData {}

    @SuppressWarnings("WeakerAccess")
    @JavaLabel(name = "Child")
    static class InheritingJavaData extends BaseJavaData {}
}
