@file:JvmName("TaskTypes")
package sandbox

import sandbox.java.lang.escapeSandbox
import sandbox.java.lang.sandbox
import sandbox.java.lang.unsandbox

typealias SandboxFunction<INPUT, OUTPUT> = sandbox.java.util.function.Function<INPUT, OUTPUT>

internal fun isEntryPoint(elt: StackTraceElement): Boolean {
    return elt.className == "sandbox.Task" && elt.methodName == "apply"
}

class Task(private val function: SandboxFunction<in Any?, out Any?>?) : SandboxFunction<Any?, Any?> {
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
class RawTask(private val function: SandboxFunction<Any?, Any?>?) : SandboxFunction<Any?, Any?> {
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
