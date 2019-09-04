package com.example.testing

import net.corda.djvm.execution.SandboxRuntimeException
import net.corda.djvm.rewiring.SandboxClassLoader
import net.corda.djvm.source.ClassSource
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.util.function.Function

class RawExecutor(private val classLoader: SandboxClassLoader) {
    private val constructor: Constructor<out Function<in Any?, out Any?>>

    init {
        val taskClass = classLoader.loadClass("sandbox.RawTask")
        @Suppress("unchecked_cast")
        constructor = taskClass.getDeclaredConstructor(classLoader.loadClass("sandbox.java.util.function.Function"))
            as Constructor<out Function<in Any?, out Any?>>
    }

    fun toSandboxClass(clazz: Class<*>): Class<*> {
        return classLoader.loadClassForSandbox(ClassSource.fromClassName(clazz.name))
    }

    fun execute(task: Any, input: Any?): Any? {
        return try {
            constructor.newInstance(task).apply(input)
        } catch (ex: Throwable) {
            val target = (ex as? InvocationTargetException)?.targetException ?: ex
            throw when (target) {
                is Error, is RuntimeException -> target
                else -> SandboxRuntimeException(target.message, target)
            }
        }
    }
}
