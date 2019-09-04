@file:JvmName("TaskTypes")
package sandbox

import sandbox.java.lang.escapeSandbox
import sandbox.java.lang.sandbox
import sandbox.java.lang.unsandbox
import java.util.Collections.unmodifiableSet

import java.util.function.Function

typealias SandboxFunction<INPUT, OUTPUT> = sandbox.java.util.function.Function<INPUT, OUTPUT>

internal fun isEntryPoint(elt: StackTraceElement): Boolean {
    return elt.methodName == "apply" && isTaskClass(elt.className)
}

private const val SANDBOX_PREFIX = "sandbox."
private val taskClasses = unmodifiableSet(setOf(
    "Task",
    "RawTask",
    "BasicInput",
    "BasicOutput",
    "ImportTask"
))

private fun isTaskClass(className: String): Boolean {
    return className.startsWith(SANDBOX_PREFIX) && className.substring(SANDBOX_PREFIX.length) in taskClasses
}

class Task(private val function: SandboxFunction<in Any?, out Any?>?) : SandboxFunction<Any?, Any?>, Function<Any?, Any?> {
    /**
     * This function runs inside the sandbox. It marshalls the input
     * object to its sandboxed equivalent, executes the user's code
     * and then marshalls the result out again.
     *
     * The marshalling should be effective for Java primitives,
     * Strings and Enums, as well as for arrays of these types.
     */
    override fun apply(input: Any?): Any? {
        val value = try {
            function?.apply(input?.sandbox())
        } catch (t: Throwable) {
            throw t.escapeSandbox()
        }
        return value?.unsandbox()
    }
}

@Suppress("unused")
class RawTask(private val function: SandboxFunction<Any?, Any?>?) : SandboxFunction<Any?, Any?>, Function<Any?, Any?> {
    /**
     * This function runs inside the sandbox, and performs NO marshalling
     * of the input and output objects. This must be done by the caller.
     */
    override fun apply(input: Any?): Any? {
        return try {
            function?.apply(input)
        } catch (t: Throwable) {
            throw t.escapeSandbox()
        }
    }
}

@Suppress("unused")
class BasicInput : SandboxFunction<Any?, Any?>, Function<Any?, Any?> {
    /**
     * This function runs inside the sandbox and
     * transforms a basic Java object into its
     * equivalent sandbox object.
     */
    override fun apply(input: Any?): Any? {
        return input?.sandbox()
    }
}

@Suppress("unused")
class BasicOutput : SandboxFunction<Any?, Any?>, Function<Any?, Any?> {
    /**
     * This function runs inside the sandbox and
     * transforms a basic sandbox object into its
     * equivalent Java object.
     */
    override fun apply(output: Any?): Any? {
        return output?.unsandbox()
    }
}

@Suppress("unused")
class ImportTask(private val function: Function<Any?, Any?>) : SandboxFunction<Any?, Any?>, Function<Any?, Any?> {
    /**
     * This allows [function] to be executed both
     * inside and outside the sandbox.
     */
    override fun apply(input: Any?): Any? {
        return function.apply(input)
    }
}
