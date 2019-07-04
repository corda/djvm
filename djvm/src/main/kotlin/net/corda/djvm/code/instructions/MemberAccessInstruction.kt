package net.corda.djvm.code.instructions

import net.corda.djvm.code.Instruction
import net.corda.djvm.references.MemberInformation

/**
 * Field access and method invocation instruction.
 *
 * @property className The class owning the field or method.
 * @property memberName The name of the field or the method being accessed.
 * @property descriptor The return type of a field or function descriptor for a method.
 * @property ownerIsInterface If the member is a method, this is true if the owner is an interface.
 */
class MemberAccessInstruction(
        operation: Int,
        override val className: String,
        override val memberName: String,
        override val descriptor: String,
        val ownerIsInterface: Boolean = false
) : Instruction(operation), MemberInformation