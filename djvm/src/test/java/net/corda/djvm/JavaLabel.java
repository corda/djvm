package net.corda.djvm;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/*
 * @sandbox.java.lang.annotation.Retention$1DJVM("RUNTIME")
 * @sandbox.java.lang.annotation.Target$1DJVM("TYPE")
 * @Documented
 * @sandbox.java.lang.annotation.Repeatable$1DJVM(sandbox.JavaLabels.class)
 * interface sandbox.JavaLabel {
 *     sandbox.String name();
 * }
 *
 * @Retention(RUNTIME)
 * @Target(TYPE)
 * @Documented
 * @Repeatable(sandbox.JavaLabels$1DJVM.class)
 * @interface sandbox.JavaLabel$1DJVM {
 *     String name();
 * }
 */
@Retention(RUNTIME)
@Target(TYPE)
@Documented
@Repeatable(JavaLabels.class)
public @interface JavaLabel {
    String name();
}
