package net.corda.djvm.code

import net.corda.djvm.code.impl.emit
import net.corda.djvm.code.impl.EMIT_DEFAULT
import net.corda.djvm.code.impl.EmitterModuleImpl

/**
 * An emitter is a hook for [ClassMutator][net.corda.djvm.code.impl.ClassMutator],
 * from where one can modify the byte code of a class method.
 */
interface Emitter {

    /**
     * Hook for providing modifications to an instruction in a method body. One can also prepend and append instructions
     * by using the [EmitterContext], and skip the default instruction altogether by invoking
     * [EmitterModuleImpl.preventDefault] from within [EmitterContext.emit].
     *
     * @param context The context from which the emitter is invoked. By calling [EmitterContext.emit], one gets access
     * to an instance of [EmitterModuleImpl] from within the supplied closure. From there, one can emit new instructions
     * and intercept the original instruction (for instance, modify or delete the instruction).
     * @param instruction The instruction currently being processed.
     */
    fun emit(context: EmitterContext, instruction: Instruction)

    /**
     * Emitters are immutable and shared. The member context represents an arbitrary
     * object that will exist only for the lifetime of the current member which the
     * [Emitter] instance can use to store "state".
     */
    @JvmDefault
    fun createMemberContext(): Any = Void.TYPE

    /**
     * Determines the order in which emitters are executed within the sandbox.
     */
    @JvmDefault
    val priority: Int
        get() = EMIT_DEFAULT

}