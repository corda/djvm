package net.corda.djvm;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target(TYPE)
@Documented
@Repeatable(JavaLabels.class)
public @interface JavaLabel {
    String name();
}
