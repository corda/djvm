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
 * interface sandbox.JavaUntrustworthy {
 *     sandbox.Untrusted value();
 * }
 *
 * @Target({TYPE, METHOD, FIELD})
 * @Retention(RUNTIME)
 * @Documented
 * @Inherited
 * @interface sandbox.JavaUntrustworthy$1DJVM {
 *     String value() default "NAUGHTY";
 * }
 */
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
@Documented
@Inherited
public @interface JavaUntrustworthy {
    Untrusted value() default Untrusted.NAUGHTY;
}
