package net.corda.djvm

import java.lang.annotation.Inherited
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.*

/*
 * @sandbox.kotlin.annotation.Target$1DJVM("CLASS", "FUNCTION", "PROPERTY", "FIELD")
 * @sandbox.kotlin.annotation.Retention$1DJVM("RUNTIME")
 * @sandbox.java.lang.annotation.Target$1DJVM("TYPE", "FIELD", "METHOD")
 * @sandbox.java.lang.annotation.Retention$1DJVM("RUNTIME")
 * @sandbox.kotlin.Metadata
 * @MustBeDocumented
 * @Documented
 * @Inherited
 * @kotlin.Metadata
 * interface sandbox.KotlinAnnotation {
 *     sandbox.String value();
 * }
 *
 * @sandbox.kotlin.annotation.Target$1DJVM("CLASS", "FUNCTION", "PROPERTY", "FIELD")
 * @sandbox.kotlin.annotation.Retention$1DJVM("RUNTIME")
 * @Target(CLASS, FUNCTION, PROPERTY, FIELD)
 * @Retention(RUNTIME)
 * @sandbox.kotlin.Metadata
 * @MustBeDocumented
 * @Documented
 * @Inherited
 * @interface sandbox.KotlinAnnotation$1DJVM {
 *     String value() default "<default-value>";
 * }
 */
@Target(CLASS, FUNCTION, PROPERTY, FIELD)
@Retention(RUNTIME)
@MustBeDocumented
@Inherited
annotation class KotlinAnnotation(
    val value: String = "<default-value>"
)
