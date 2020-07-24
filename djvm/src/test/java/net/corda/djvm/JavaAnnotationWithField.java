package net.corda.djvm;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Arrays.asList;

/*
 * @sandbox.java.lang.annotation.Target$1DJVM({"TYPE", "CONSTRUCTOR", "METHOD", "FIELD", "PACKAGE})
 * @sandbox.java.lang.annotation.Retention$1DJVM("RUNTIME")
 * @Documented
 * @Inherited
 * interface sandbox.JavaAnnotationWithField {
 *     sandbox.java.util.List<String> data = asList(DJVM.intern("Hello"), DJVM.intern("Sandbox"));
 * }
 *
 * @Target({TYPE, CONSTRUCTOR, METHOD, FIELD, PACKAGE})
 * @Retention(RUNTIME)
 * @Documented
 * @Inherited
 * @interface sandbox.JavaAnnotation$1DJVM {
 * }
 */
@Target({TYPE, CONSTRUCTOR, METHOD, FIELD, PACKAGE})
@Retention(RUNTIME)
@Documented
@Inherited
public @interface JavaAnnotationWithField {
    List<String> data = asList("Hello", "Sandbox");
}
