package net.corda.djvm;

import java.io.IOException;
import java.lang.annotation.*;
import java.util.Collection;
import java.util.List;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/*
 * @sandbox.java.lang.annotation.Target$1DJVM({"TYPE", "METHOD", "FIELD"})
 * @sandbox.java.lang.annotation.Retention$1DJVM("RUNTIME")
 * @Documented
 * @Inherited
 * interface sandbox.JavaAnnotationClassData {
 *     Class<? extends sandbox.Annotation> annotationClass();
 *     Class<? extends sandbox.Throwable> throwableClass();
 *     Class<? extends sandbox.Enum<?>> enumClass();
 *     Class<? extends sandbox.Collection> collectionClass();
 *     Class<?> anyOldClass();
 * }
 *
 * @Target({TYPE, METHOD, FIELD})
 * @Retention(RUNTIME)
 * @Documented
 * @Inherited
 * @interface sandbox.JavaAnnotationClassData$1DJVM {
 *     Class<?> annotationClass() default sandbox.JavaAnnotationClassData.class;
 *     Class<?> throwableClass() default sandbox.IOException.class;
 *     Class<?> enumClass() default sandbox.Label.class;
 *     Class<?> collectionClass() default sandbox.List.class;
 *     Class<?> anyOldClass() default Class.class;
 * }
 */
@SuppressWarnings("unused")
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
@Documented
@Inherited
public @interface JavaAnnotationClassData {
    Class<? extends Annotation> annotationClass() default JavaAnnotationClassData.class;
    Class<? extends Throwable> throwableClass() default IOException.class;
    Class<? extends Enum<?>> enumClass() default Label.class;
    Class<? extends Collection> collectionClass() default List.class;
    Class<?> anyOldClass() default Class.class;
}
