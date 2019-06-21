package net.corda.djvm.rules.implementation

import net.corda.djvm.code.Emitter
import net.corda.djvm.code.EmitterContext
import net.corda.djvm.code.EmitterModule
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
    private const val SUN_SECURITY_PROVIDERS = "sun/security/jca/Provider"
    private val MONITOR_METHODS = setOf("notify", "notifyAll", "wait")
    private val CLASSLOADING_METHODS = setOf("defineClass", "findClass")
    private val memberFormatter = MemberFormatter()

    override fun emit(context: EmitterContext, instruction: Instruction) = context.emit {
        val className = (context.member ?: return).className
        if (instruction is MemberAccessInstruction && instruction.isMethod) {
            when (instruction.operation) {
                INVOKEVIRTUAL, INVOKESPECIAL -> if (isObjectMonitor(instruction)) {
                    forbid(instruction)
                } else {
                    when (Enforcer(instruction).runFor(className)) {
                        Choice.FORBID -> forbid(instruction)
                        Choice.LOAD_CLASS -> loadClass()
                        else -> Unit
                    }
                }

                INVOKESTATIC -> if (instruction.owner == "java/lang/ClassLoader") {
                    if (instruction.memberName == "getSystemClassLoader") {
                        invokeStatic("sandbox/java/lang/DJVM", instruction.memberName, instruction.descriptor)
                        preventDefault()
                    } else {
                        forbid(instruction)
                    }
                }
            }
        }
    }

    private fun EmitterModule.forbid(instruction: MemberAccessInstruction) {
        throwException<RuleViolationError>("Disallowed reference to API; ${memberFormatter.format(instruction.member)}")
        preventDefault()
    }

    private fun EmitterModule.loadClass() {
        invokeStatic("sandbox/java/lang/DJVM", "loadClass", "(Ljava/lang/ClassLoader;Ljava/lang/String;)Ljava/lang/Class;")
        preventDefault()
    }

    private fun isObjectMonitor(instruction: MemberAccessInstruction): Boolean =
        (instruction.descriptor == "()V" && instruction.memberName in MONITOR_METHODS)
            || (instruction.memberName == "wait" && (instruction.descriptor == "(J)V" || instruction.descriptor == "(JI)V"))

    private enum class Choice {
        FORBID,
        LOAD_CLASS,
        PASS
    }

    private class Enforcer(private val instruction: MemberAccessInstruction) {
        private val isClassLoader: Boolean = instruction.owner == "java/lang/ClassLoader"
        private val isClass: Boolean = instruction.owner == "java/lang/Class"
        private val hasClassReflection: Boolean = isClass && instruction.descriptor.contains("Ljava/lang/reflect/")
        private val isNewInstance: Boolean = instruction.memberName == "newInstance" &&
            (isClass && instruction.descriptor == "()Ljava/lang/Object;") || instruction.owner == "java/lang/reflect/Constructor"
        private val isLoadClass: Boolean = instruction.memberName == "loadClass"
        private val hasAnyClassReflection = hasClassReflection || isNewInstance

        fun runFor(className: String): Choice = when {
            // Required to load character sets.
            className.startsWith(CHARSET_PACKAGE) -> when {
                hasClassReflection || hasAnyClassLoading() -> Choice.FORBID
                else -> Choice.PASS
            }

            // These two are required to load security providers.
            className.startsWith(SUN_SECURITY_PROVIDERS) -> when {
                hasClassReflection -> Choice.FORBID
                else -> allowLoadClass()
            }
            className == "java/security/Provider\$Service" -> allowLoadClass()

            // Forbid classloading and reflection otherwise.
            hasAnyClassReflection || hasAnyClassLoading() -> Choice.FORBID

            else -> Choice.PASS
        }

        private fun allowLoadClass(): Choice = when {
            !isClassLoader -> Choice.PASS
            isLoadClass -> Choice.LOAD_CLASS
            instruction.memberName in CLASSLOADING_METHODS -> Choice.FORBID
            else -> Choice.PASS
        }

        private fun hasAnyClassLoading(): Boolean =
                isClassLoader && (isLoadClass || instruction.memberName in CLASSLOADING_METHODS)
    }
}