package net.corda.djvm.rules.implementation

import net.corda.djvm.code.*
import net.corda.djvm.code.instructions.MemberAccessInstruction
import org.objectweb.asm.Opcodes.*

/**
 * We cannot wrap Java array objects - and potentially others - and so these would still
 * use the non-deterministic [java.lang.Object.hashCode] by default. Therefore we intercept
 * these invocations and redirect them to our [sandbox.java.lang.DJVM] object.
 *
 * And similarly for [java.lang.Object.toString], which needs to return an instance of
 * [sandbox.java.lang.String] inside the sandbox, even for objects which cannot extend
 * [sandbox.java.lang.Object].
 */
object RewriteObjectMethods : Emitter {
    override fun emit(context: EmitterContext, instruction: Instruction) = context.emit {
        if (instruction is MemberAccessInstruction && instruction.className == OBJECT_NAME) {
            when (instruction.operation) {
                INVOKEVIRTUAL ->
                    if (instruction.memberName == "hashCode" && instruction.descriptor == "()I") {
                        invokeStatic(
                            owner = DJVM_NAME,
                            name = "hashCode",
                            descriptor = "(Ljava/lang/Object;)I"
                        )
                        preventDefault()
                    } else if (instruction.memberName == "toString" && instruction.descriptor == "()Ljava/lang/String;") {
                        invokeStatic(
                            owner = DJVM_NAME,
                            name = "toString",
                            descriptor = "(Ljava/lang/Object;)Lsandbox/java/lang/String;"
                        )
                        preventDefault()
                    }
            }
        }
    }
}
