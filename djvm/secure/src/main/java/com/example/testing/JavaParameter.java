package com.example.testing;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Repeatable(JavaParameters.class)
@Retention(RUNTIME)
@Target(PARAMETER)
@Documented
public @interface JavaParameter {
    JavaTag value();
}
