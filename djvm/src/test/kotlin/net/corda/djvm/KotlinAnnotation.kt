package net.corda.djvm

import java.lang.annotation.Inherited
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.*

@Target(CLASS, FUNCTION, PROPERTY, FIELD)
@Retention(RUNTIME)
@MustBeDocumented
@Inherited
annotation class KotlinAnnotation(
    val value: String = "<default-value>"
)
