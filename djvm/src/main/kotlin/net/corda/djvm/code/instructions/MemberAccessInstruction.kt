package net.corda.djvm.code.instructions

import net.corda.djvm.code.Instruction
import net.corda.djvm.references.MemberReference

/**
 * Field access and method invocation instruction.
 *
 * @property owner  The class owning the field or method.
 * @property memberName The name of the field or the method being accessed.
 * @property descriptor The return type of a field or function descriptor for a method.
 * @property ownerIsInterface If the member is a method, this is true if the owner is an interface.
 * @property isMethod Indicates whether the member is a method or a field.
 */
class MemberAccessInstruction(
        operation: Int,
        val owner: String,
        val memberName: String,
        val descriptor: String,
        val ownerIsInterface: Boolean = false,
        val isMethod: Boolean = false
) : Instruction(operation) {

    /**
     * The absolute name of the referenced member.
     */
    val reference = "$owner.$memberName:$descriptor"

    /**
     * Get a member reference representation of the target of the instruction.
     */
    val member: MemberReference
        get() = MemberReference(owner, memberName, descriptor)

}
