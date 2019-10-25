package net.corda.djvm

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS

@Retention(RUNTIME)
@Target(CLASS)
annotation class KotlinAnnotation(
    val value: String = "<default-value>"
)
