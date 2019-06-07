package net.corda.djvm.code.instructions

import net.corda.djvm.code.Instruction
import org.objectweb.asm.Opcodes

/**
 * Dynamic invocation instruction.
 *
 * @property memberName The name of the method to invoke.
 * @property descriptor The function descriptor of the method being invoked.
 * @property numberOfArguments The number of arguments to pass to the target.
 * @property returnsValueOrReference False if the target returns `void`, or true if it returns a value or a reference.
 */
@Suppress("MemberVisibilityCanBePrivate")
class DynamicInvocationInstruction(
        val memberName: String,
        val descriptor: String,
        val numberOfArguments: Int,
        val returnsValueOrReference: Boolean
) : Instruction(Opcodes.INVOKEDYNAMIC)
