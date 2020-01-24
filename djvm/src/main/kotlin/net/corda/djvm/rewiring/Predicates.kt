@file:JvmName("Predicates")
package net.corda.djvm.rewiring

import net.corda.djvm.execution.SandboxRuntimeException
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.util.function.Function
import java.util.function.Predicate

/**
 * Returns an instance of [Function] that can wrap an
 * instance of [sandbox.java.util.function.Predicate].
 * The predicate's input is not marshalled.
 */
@Throws(
    ClassNotFoundException::class,
    NoSuchMethodException::class
)
fun SandboxClassLoader.createRawPredicateFactory(): Function<in Any, out Predicate<in Any?>> {
    val taskClass = loadClass("sandbox.PredicateTask")
    @Suppress("unchecked_cast")
    val constructor = taskClass.getDeclaredConstructor(loadClass("sandbox.java.util.function.Predicate"))
            as Constructor<out Predicate<in Any?>>
    return Function { userPredicate ->
        try {
            constructor.newInstance(userPredicate)
        } catch (ex: Throwable) {
            val target = (ex as? InvocationTargetException)?.targetException ?: ex
            throw when (target) {
                is RuntimeException, is Error -> target
                else -> SandboxRuntimeException(target.message, target)
            }
        }
    }
}

/**
 * Factory to create a [Function] that will execute a sandboxed
 * instance of a task, where this task implements [Predicate].
 * This is just a convenience function which assumes that the
 * task has a no-argument constructor, but is still likely to be
 * what you want.
 */
fun SandboxClassLoader.createSandboxPredicate(): Function<Class<out Predicate<*>>, out Any> {
    return Function { predicateClass ->
        try {
            toSandboxClass(predicateClass).getDeclaredConstructor().newInstance()
        } catch (ex: Throwable) {
            val target = (ex as? InvocationTargetException)?.targetException ?: ex
            throw when (target) {
                is RuntimeException, is Error -> target
                else -> SandboxRuntimeException(target.message, target)
            }
        }
    }
}
