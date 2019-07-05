package net.corda.djvm.rules.implementation

import jdk.internal.org.objectweb.asm.Type
import net.corda.djvm.code.Emitter
import net.corda.djvm.code.EmitterContext
import net.corda.djvm.code.EmitterModule
import net.corda.djvm.code.Instruction
import net.corda.djvm.code.instructions.DynamicInvocationInstruction
import net.corda.djvm.references.MemberReference
import org.objectweb.asm.Handle

/**
 * Checks for invalid dynamic invocations.
 */
object DisallowDynamicInvocation : Emitter {

    override fun emit(context: EmitterContext, instruction: Instruction) = context.emit {
        if (instruction is DynamicInvocationInstruction
            && instruction.bootstrap.owner == "java/lang/invoke/LambdaMetafactory"
            && isMetafactory(instruction.bootstrap.name)
            && !isJvmLambda(instruction.bootstrapArgs)) {
            //forbid(instruction)
            // TODO Allow specific lambda and string concatenation meta-factories used by Java code itself
        }
    }

    private fun isMetafactory(methodName: String): Boolean {
        return methodName == "metafactory" || methodName == "altMetafactory"
    }

    private fun isJvmLambda(bootstrapArgs: Array<out Any?>): Boolean {
        return (bootstrapArgs.size > 1) && isJvmInternal((bootstrapArgs[1] as? Handle ?: return false).owner)
    }

    private fun isJvmInternal(className: String): Boolean {
        return className.startsWith("java/")
    }

    private fun EmitterModule.forbid(instruction: DynamicInvocationInstruction) {
        val lambdaType = Type.getObjectType(instruction.descriptor)
        val dynamicMember = MemberReference(
            className = lambdaType.returnType.className,
            memberName = instruction.memberName,
            descriptor = instruction.descriptor
        )
        throwRuleViolationError("Disallowed reference to Lambda; ${formatFor(dynamicMember)}")
    }
}
