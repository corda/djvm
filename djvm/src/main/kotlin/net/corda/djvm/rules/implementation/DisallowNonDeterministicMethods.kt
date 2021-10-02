package net.corda.djvm.rules.implementation

import net.corda.djvm.code.Emitter
import net.corda.djvm.code.EmitterContext
import net.corda.djvm.code.Instruction
import net.corda.djvm.code.impl.CLASSLOADER_NAME
import net.corda.djvm.code.impl.CLASS_NAME
import net.corda.djvm.code.impl.CONSTRUCTOR_NAME
import net.corda.djvm.code.impl.DJVM_NAME
import net.corda.djvm.code.impl.EmitterModuleImpl
import net.corda.djvm.code.impl.emit
import net.corda.djvm.code.impl.isClassVirtualThunk
import net.corda.djvm.code.impl.isObjectMonitor
import net.corda.djvm.code.instructions.MemberAccessInstruction
import org.objectweb.asm.Opcodes.*

/**
 * Some non-deterministic APIs belong to whitelisted classes and so cannot be stubbed out.
 * Replace their invocations with safe alternatives, e.g. throwing an exception.
 */
object DisallowNonDeterministicMethods : Emitter {

    private val CLASSLOADING_METHODS = setOf("defineClass", "findClass")

    override fun emit(context: EmitterContext, instruction: Instruction) = context.emit {
        if (instruction is MemberAccessInstruction && instruction.isMethod) {
            when (instruction.operation) {
                INVOKEVIRTUAL, INVOKESPECIAL ->
                    if (isObjectMonitor(instruction)) {
                        forbid(instruction)
                    } else {
                        when (Enforcer(instruction).run()) {
                            Choice.FORBID -> forbid(instruction)
                            Choice.INIT_CLASSLOADER -> initClassLoader()
                            else -> Unit
                        }
                    }
            }
        }
    }

    private fun EmitterModuleImpl.forbid(instruction: MemberAccessInstruction) {
        throwRuleViolationError("Disallowed reference to API; ${formatFor(instruction)}")
        preventDefault()
    }

    private fun EmitterModuleImpl.initClassLoader() {
        invokeStatic(DJVM_NAME, "getSystemClassLoader", "()Ljava/lang/ClassLoader;")
        invokeSpecial(CLASSLOADER_NAME, CONSTRUCTOR_NAME, "(Ljava/lang/ClassLoader;)V")
        preventDefault()
    }

    private fun isObjectMonitor(instruction: MemberAccessInstruction): Boolean
        = isObjectMonitor(instruction.memberName, instruction.descriptor)

    private enum class Choice {
        FORBID,
        INIT_CLASSLOADER,
        PASS
    }

    private class Enforcer(private val instruction: MemberAccessInstruction) {
        private val isClassLoader: Boolean = instruction.className == CLASSLOADER_NAME
        private val isLoadClass: Boolean = instruction.memberName == "loadClass"

        private val hasClassReflection: Boolean get() = instruction.className == CLASS_NAME
            && instruction.descriptor.contains("Ljava/lang/reflect/")
            && !isClassVirtualThunk(instruction.memberName)

        fun run(): Choice = when {
            isClassLoader && instruction.memberName == CONSTRUCTOR_NAME ->
                if (instruction.descriptor == "()V") {
                    Choice.INIT_CLASSLOADER
                } else {
                    Choice.FORBID
                }

            // Forbid any reflection on Class<*> we cannot handle.
            hasClassReflection -> Choice.FORBID

            else -> allowLoadClass()
        }

        private fun allowLoadClass(): Choice = when {
            !isClassLoader -> Choice.PASS
            isLoadClass -> when (instruction.descriptor) {
                "(Ljava/lang/String;)Ljava/lang/Class;" -> Choice.PASS
                else -> Choice.FORBID
            }
            instruction.memberName in CLASSLOADING_METHODS -> Choice.FORBID
            else -> Choice.PASS
        }
    }
}
