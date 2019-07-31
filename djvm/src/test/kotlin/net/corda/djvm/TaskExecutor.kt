package net.corda.djvm

import net.corda.djvm.rewiring.SandboxClassLoader
import net.corda.djvm.source.ClassSource
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

class TaskExecutor(private val classLoader: SandboxClassLoader) {
    private val constructor: Constructor<out Any>
    private val executeMethod: Method

    init {
        val taskClass = classLoader.loadClass("sandbox.Task")
        constructor = taskClass.getDeclaredConstructor(classLoader.loadClass("sandbox.java.util.function.Function"))
        executeMethod = taskClass.getMethod("apply", Any::class.java)
    }

    fun toSandboxClass(clazz: Class<*>): Class<*> {
        return classLoader.loadClassForSandbox(ClassSource.fromClassName(clazz.name))
    }

    fun execute(task: Any, input: Any?): Any? {
        return try {
            executeMethod.invoke(constructor.newInstance(task), input)
        } catch (ex: InvocationTargetException) {
            throw ex.targetException
        }
    }
}
