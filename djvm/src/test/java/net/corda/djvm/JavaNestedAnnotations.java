package net.corda.djvm;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/*
 * @sandbox.java.lang.annotation.Target$1DJVM({"TYPE", "METHOD", "FIELD"})
 * @sandbox.java.lang.annotation.Retention$1DJVM("RUNTIME")
 * @Documented
 * @Inherited
 * interface sandbox.JavaNestedAnnotations {
 *     sandbox.JavaAnnotation annotationData();
 *     sandbox.JavaAnnotation[] annotationsData();
 * }
 *
 * @Target({TYPE, METHOD, FIELD})
 * @Retention(RUNTIME)
 * @Documented
 * @Inherited
 * @interface sandbox.JavaNestedAnnotations$1DJVM {
 *     sandbox.JavaAnnotation$1DJVM annotationData() default @sandbox.JavaAnnotation$1DJVM("<empty>");
 *     sandbox.JavaAnnotation$1DJVM[] annotationsData() default {};
 * }
 */
@SuppressWarnings("unused")
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
@Documented
@Inherited
public @interface JavaNestedAnnotations {
    JavaAnnotation annotationData() default @JavaAnnotation("<empty>");
    JavaAnnotation[] annotationsData() default {};
}
