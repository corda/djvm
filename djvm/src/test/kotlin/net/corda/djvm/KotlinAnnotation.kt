package net.corda.djvm

import kotlin.annotation.AnnotationRetention.*
import kotlin.annotation.AnnotationTarget.*

@Retention(RUNTIME)
@Target(CLASS)
annotation class KotlinAnnotation
