package net.corda.djvm

import java.util.function.Function

@FunctionalInterface
interface TypedTaskFactory {
    fun <T, R> create(taskClass: Class<out Function<T, R>>): Function<T, R>
}
