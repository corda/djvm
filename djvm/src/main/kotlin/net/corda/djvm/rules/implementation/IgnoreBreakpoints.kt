package net.corda.djvm.rules.implementation

import net.corda.djvm.code.Emitter
import net.corda.djvm.code.EmitterContext
import net.corda.djvm.code.Instruction
import net.corda.djvm.code.Instruction.Companion.OP_BREAKPOINT
import net.corda.djvm.code.impl.emit

/**
 * Rule that deletes invalid breakpoint instructions.
 */
object IgnoreBreakpoints : Emitter {

    override fun emit(context: EmitterContext, instruction: Instruction) = context.emit {
        when (instruction.operation) {
            OP_BREAKPOINT -> preventDefault()
        }
    }

}
