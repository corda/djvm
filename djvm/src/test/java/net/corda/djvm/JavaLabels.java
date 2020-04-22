package net.corda.djvm;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/*
 * @sandbox.java.lang.annotation.Retention$1DJVM("RUNTIME")
 * @sandbox.java.lang.annotation.Target$1DJVM("TYPE")
 * @Documented
 * interface sandbox.JavaLabels {
 *     sandbox.JavaLabel[] value();
 * }
 *
 * @Retention(RUNTIME)
 * @Target(TYPE)
 * @Documented
 * @interface sandbox.JavaLabels$1DJVM {
 *     sandbox.JavaLabel$1DJVM[] value();
 * }
 */
@Retention(RUNTIME)
@Target(TYPE)
@Documented
public @interface JavaLabels {
    JavaLabel[] value();
}
