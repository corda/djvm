package net.corda.djvm;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

/*
 * @sandbox.java.lang.annotation.Target$1DJVM({"TYPE", "CONSTRUCTOR", "METHOD", "FIELD"})
 * @sandbox.java.lang.annotation.Retention$1DJVM("RUNTIME")
 * @Documented
 * @Inherited
 * interface sandbox.JavaAnnotation {
 *     sandbox.String value();
 * }
 *
 * @Target({TYPE, CONSTRUCTOR, METHOD, FIELD})
 * @Retention(RUNTIME)
 * @Documented
 * @Inherited
 * @interface sandbox.JavaAnnotation$1DJVM {
 *     String value() default "<default-value>";
 * }
 */
@Target({TYPE, CONSTRUCTOR, METHOD, FIELD})
@Retention(RUNTIME)
@Documented
@Inherited
public @interface JavaAnnotation {
    String value() default "<default-value>";
}
