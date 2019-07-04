package net.corda.djvm.code.instructions

import net.corda.djvm.code.Instruction
import net.corda.djvm.references.MemberInformation
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes

/**
 * Dynamic invocation instruction.
 *
 * @property method The method containing the dynamic invocation.
 * @property memberName The name of the method to invoke.
 * @property descriptor The function descriptor of the method being invoked.
 * @property bootstrap The [Handle] for the bootstrap method.
 * @property bootstrapArgs The arguments to pass to the bootstrap method.
 */
@Suppress("MemberVisibilityCanBePrivate")
class DynamicInvocationInstruction(
    val method: MemberInformation,
    val memberName: String,
    val descriptor: String,
    val bootstrap: Handle,
    var bootstrapArgs: Array<out Any?>
) : Instruction(Opcodes.INVOKEDYNAMIC)
