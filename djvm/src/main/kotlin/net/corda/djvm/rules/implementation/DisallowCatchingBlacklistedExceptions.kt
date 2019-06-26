package net.corda.djvm.rules.implementation

import net.corda.djvm.code.*
import net.corda.djvm.code.instructions.CodeLabel
import net.corda.djvm.code.instructions.TryCatchBlock
import net.corda.djvm.costing.ThresholdViolationError
import org.objectweb.asm.Label

/**
 * Rule that checks for attempted catches of [ThreadDeath], [ThresholdViolationError],
 * [StackOverflowError], [OutOfMemoryError], [Error] or [Throwable].
 */
object DisallowCatchingBlacklistedExceptions : Emitter {

    private val disallowedExceptionTypes = setOf(
        ruleViolationError,
        thresholdViolationError,

        /**
         * These errors indicate that the JVM is failing,
         * so don't allow these to be caught either.
         */
        "java/lang/StackOverflowError",
        "java/lang/OutOfMemoryError",
        "java/lang/InternalError",

        /**
         * These are immediate super-classes for our explicit errors.
         */
        "java/lang/VirtualMachineError",
        "java/lang/ThreadDeath",

        /**
         * Any of [ThreadDeath] and [VirtualMachineError]'s throwable
         * super-classes also need explicit checking.
         */
        THROWABLE_NAME,
        "java/lang/Error"
    )

    override fun createMemberContext() = mutableSetOf<Label>()

    override fun emit(context: EmitterContext, instruction: Instruction) = context.emit {
        val exceptionHandlers: MutableSet<Label> = getMemberContext(context) ?: return
        if (instruction is TryCatchBlock && instruction.typeName in disallowedExceptionTypes) {
            exceptionHandlers.add(instruction.handler)
        } else if (instruction is CodeLabel && instruction.label in exceptionHandlers) {
            duplicate()
            invokeInstrumenter("checkCatch", "(Ljava/lang/Throwable;)V")
        }
    }

    /**
     * We need to invoke this emitter before the [HandleExceptionUnwrapper]
     * so that we don't unwrap exceptions we don't want to catch.
     */
    override val priority: Int
        get() = EMIT_TRAPPING_EXCEPTIONS
}
