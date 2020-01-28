package net.corda.djvm

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS

@Retention(RUNTIME)
@Target(CLASS)
@MustBeDocumented
@Repeatable
annotation class KotlinLabel(val name: String)
