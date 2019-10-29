package net.corda.djvm;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

@Target({
   TYPE,
   FIELD,
   METHOD,
   CONSTRUCTOR
})
@Retention(CLASS)
public @interface CordaInternal {
}
