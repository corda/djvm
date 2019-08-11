package com.example.testing

import net.corda.djvm.rewiring.SandboxClassLoader
import net.corda.djvm.source.ClassSource
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

class Executor(private val classLoader: SandboxClassLoader) {
    private val constructor: Constructor<out Any>
    private val executeMethod: Method

    init {
        val taskClass = classLoader.loadClass("sandbox.RawTask")
        constructor = taskClass.getDeclaredConstructor(classLoader.loadClass("sandbox.java.util.function.Function"))
        executeMethod = taskClass.getDeclaredMethod("apply", Any::class.java)
    }

    fun toSandboxClass(clazz: Class<*>): Class<*> {
        return classLoader.loadClassForSandbox(ClassSource.fromClassName(clazz.name))
    }

    fun execute(task: Any, input: Any?): Any? {
        return try {
            executeMethod(constructor.newInstance(task), input)
        } catch (ex: InvocationTargetException) {
            throw ex.targetException
        }
    }
}
