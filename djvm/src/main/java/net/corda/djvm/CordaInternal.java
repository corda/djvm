package net.corda.djvm;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Apply this annotation to elements that we want
 * the API Scanner Gradle plugin to ignore.
 */
@Target({
   TYPE,
   FIELD,
   METHOD,
   CONSTRUCTOR
})
@Retention(CLASS)
public @interface CordaInternal {
}
