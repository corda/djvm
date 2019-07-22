package net.corda.djvm.rules.implementation

import net.corda.djvm.analysis.AnalysisRuntimeContext
import net.corda.djvm.code.*
import net.corda.djvm.code.instructions.MemberAccessInstruction
import net.corda.djvm.code.instructions.TypeInstruction
import net.corda.djvm.references.ClassRepresentation
import org.objectweb.asm.Opcodes
import java.lang.reflect.Modifier

/**
 * Definition provider that ensures that all objects inherit from a sandboxed version of [java.lang.Object], with a
 * deterministic `hashCode()` method.
 */
object AlwaysInheritFromSandboxedObject : ClassDefinitionProvider, Emitter {

    private const val SANDBOX_OBJECT_NAME = "sandbox/java/lang/Object"

    private const val CONSTRUCTOR_NAME = "<init>"

    override fun define(context: AnalysisRuntimeContext, clazz: ClassRepresentation) = when {
        isDirectSubClassOfObject(context.clazz) -> clazz.copy(superClass = SANDBOX_OBJECT_NAME)
        else -> clazz
    }

    override fun emit(context: EmitterContext, instruction: Instruction) = context.emit {
        if (instruction is TypeInstruction &&
                instruction.typeName == OBJECT_NAME &&
                instruction.operation != Opcodes.ANEWARRAY &&
                instruction.operation != Opcodes.MULTIANEWARRAY) {
            // When creating new objects, make sure the sandboxed type gets used.
            // However, an array is always [java.lang.Object] so we must exclude
            // arrays from this so that we can still support arrays of arrays.
            new(SANDBOX_OBJECT_NAME, instruction.operation)
            preventDefault()
        }
        if (instruction is MemberAccessInstruction &&
                instruction.operation == Opcodes.INVOKESPECIAL &&
                instruction.className == OBJECT_NAME &&
                instruction.memberName == CONSTRUCTOR_NAME &&
                context.clazz.name != SANDBOX_OBJECT_NAME) {
            // Rewrite object initialisation call so that the sandboxed constructor gets used instead.
            invokeSpecial(SANDBOX_OBJECT_NAME, CONSTRUCTOR_NAME, "()V", instruction.ownerIsInterface)
            preventDefault()
        }
    }

    private fun isDirectSubClassOfObject(clazz: ClassRepresentation): Boolean {
        // Check if the super class is java.lang.Object and that current class is not sandbox.java.lang.Object.
        val isClass = !Modifier.isInterface(clazz.access)
        return isClass && isObject(clazz.superClass) && clazz.name != SANDBOX_OBJECT_NAME
    }

    private fun isObject(superClass: String) = superClass.isBlank() || superClass == OBJECT_NAME

}
