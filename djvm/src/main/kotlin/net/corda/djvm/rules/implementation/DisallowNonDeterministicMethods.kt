package net.corda.djvm.rules.implementation

import net.corda.djvm.code.Emitter
import net.corda.djvm.code.EmitterContext
import net.corda.djvm.code.Instruction
import net.corda.djvm.code.instructions.MemberAccessInstruction
import net.corda.djvm.formatting.MemberFormatter
import org.objectweb.asm.Opcodes.*
import sandbox.net.corda.djvm.rules.RuleViolationError

/**
 * Some non-deterministic APIs belong to pinned classes and so cannot be stubbed out.
 * Replace their invocations with exceptions instead.
 */
object DisallowNonDeterministicMethods : Emitter {

    private const val CHARSET_PACKAGE = "sun/nio/cs/"
    private val MONITOR_METHODS = setOf("notify", "notifyAll", "wait")
    private val CLASSLOADING_METHODS = setOf("defineClass", "loadClass", "findClass")
    private val memberFormatter = MemberFormatter()

    override fun emit(context: EmitterContext, instruction: Instruction) = context.emit {
        val className = (context.member ?: return).className
        if (instruction is MemberAccessInstruction && isForbidden(className, instruction)) {
            when (instruction.operation) {
                INVOKEVIRTUAL, INVOKESPECIAL -> {
                    throwException<RuleViolationError>("Disallowed reference to API; ${memberFormatter.format(instruction.member)}")
                    preventDefault()
                }
            }
        }
    }

    private fun isClassReflection(className: String, instruction: MemberAccessInstruction): Boolean =
            (instruction.owner == "java/lang/Class") && (
                instruction.descriptor.contains("Ljava/lang/reflect/") ||
                    (!className.startsWith(CHARSET_PACKAGE) && instruction.memberName == "newInstance" && instruction.descriptor == "()Ljava/lang/Object;")
            )

    private fun isClassLoading(instruction: MemberAccessInstruction): Boolean =
            (instruction.owner == "java/lang/ClassLoader") && instruction.memberName in CLASSLOADING_METHODS

    private fun isObjectMonitor(instruction: MemberAccessInstruction): Boolean =
            (instruction.descriptor == "()V" && instruction.memberName in MONITOR_METHODS)
                    || (instruction.memberName == "wait" && (instruction.descriptor == "(J)V" || instruction.descriptor == "(JI)V"))

    private fun isForbidden(className: String, instruction: MemberAccessInstruction): Boolean
            = instruction.isMethod && (isClassReflection(className, instruction) || isObjectMonitor(instruction) || isClassLoading(instruction))

}