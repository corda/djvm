@file:JvmName("TaskTypes")
@file:Suppress("unused")
package sandbox

import sandbox.java.lang.checkCatch
import sandbox.java.lang.escapeSandbox
import sandbox.java.lang.sandbox
import sandbox.java.lang.toRuleViolationError
import sandbox.java.lang.toRuntimeException
import sandbox.java.lang.unsandbox
import java.util.Collections.unmodifiableSet

import java.util.function.Function
import java.util.function.Predicate
import java.util.function.Supplier

typealias SandboxFunction<INPUT, OUTPUT> = sandbox.java.util.function.Function<INPUT, OUTPUT>
typealias SandboxPredicate<INPUT> = sandbox.java.util.function.Predicate<INPUT>
typealias SandboxSupplier<OUTPUT> = sandbox.java.util.function.Supplier<OUTPUT>

fun isEntryPoint(elt: StackTraceElement): Boolean {
    return elt.methodName == "apply" && isTaskClass(elt.className)
}

private const val SANDBOX_PREFIX = "sandbox."
private val taskClasses = unmodifiableSet(setOf(
    "Task",
    "RawTask",
    "BasicInput",
    "BasicOutput",
    "PredicateTask"
))

private fun isTaskClass(className: String): Boolean {
    return className.startsWith(SANDBOX_PREFIX) && className.substring(SANDBOX_PREFIX.length) in taskClasses
}

class Task(private val function: SandboxFunction<in Any?, out Any?>?) : SandboxFunction<Any?, Any?>, Function<Any?, Any?> {
    /**
     * This [function] runs inside the sandbox. It marshalls the [input]
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

class RawTask(private val function: SandboxFunction<in Any?, out Any?>?) : SandboxFunction<Any?, Any?>, Function<Any?, Any?> {
    /**
     * This [function] runs inside the sandbox, and performs NO marshalling
     * of the [input] and output objects. This must be done by the caller.
     */
    override fun apply(input: Any?): Any? {
        return try {
            function?.apply(input)
        } catch (t: Throwable) {
            throw t.escapeSandbox()
        }
    }
}

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

class ImportTask(private val function: Function<in Any?, out Any?>) : SandboxFunction<Any?, Any?>, Function<Any?, Any?> {
    /**
     * This allows [function] to be executed inside the sandbox.
     * !!! USE WITH EXTREME CARE !!!
     */
    override fun apply(input: Any?): Any? {
        return try {
            function.apply(input)
        } catch (e: Exception) {
            throw e.toRuntimeException()
        } catch (t: Throwable) {
            checkCatch(t)
            throw t.toRuleViolationError()
        }
    }
}

class ImportSupplierTask(private val supplier: Supplier<out Any?>) : SandboxSupplier<Any?>, Supplier<Any?> {
    /**
     * This allows [supplier] to be executed inside the sandbox.
     * !!! USE WITH EXTREME CARE !!!
     */
    override fun get(): Any? {
        return try {
            supplier.get()
        } catch (e: Exception) {
            throw e.toRuntimeException()
        } catch (t: Throwable) {
            checkCatch(t)
            throw t.toRuleViolationError()
        }
    }
}

class PredicateTask(private val predicate: SandboxPredicate<in Any?>) : SandboxPredicate<Any?>, Predicate<Any?> {
    /**
     * This [predicate] runs inside the sandbox, and performs NO marshalling
     * of the [input] object. This must be done by the caller.
     */
    override fun test(input: Any?): Boolean {
        return try {
            predicate.test(input)
        } catch (t: Throwable) {
            throw t.escapeSandbox()
        }
    }
}
