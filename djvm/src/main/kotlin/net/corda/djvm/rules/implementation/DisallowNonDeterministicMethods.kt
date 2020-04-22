package net.corda.djvm.rules.implementation

import net.corda.djvm.code.*
import net.corda.djvm.code.instructions.MemberAccessInstruction
import org.objectweb.asm.Opcodes.*

/**
 * Some non-deterministic APIs belong to whitelisted classes and so cannot be stubbed out.
 * Replace their invocations with safe alternatives, e.g. throwing an exception.
 */
object DisallowNonDeterministicMethods : Emitter {

    private val ALLOWED_GETTERS = setOf(
        "getConstructor",
        "getConstructors",
        "getEnclosingConstructor",
        "getMethod",
        "getMethods",
        "getEnclosingMethod"
    )
    private val CLASSLOADING_METHODS = setOf("defineClass", "findClass")
    private val NEW_INSTANCE_CLASSES = setOf(
        "java/security/Provider\$Service",
        "sun/security/x509/CertificateExtensions",
        "sun/security/x509/CRLExtensions",
        "sun/security/x509/OtherName"
    )

    override fun emit(context: EmitterContext, instruction: Instruction) = context.emit {
        val className = (context.member ?: return).className
        if (instruction is MemberAccessInstruction && instruction.isMethod) {
            when (instruction.operation) {
                INVOKEVIRTUAL, INVOKESPECIAL ->
                    if (isObjectMonitor(instruction)) {
                        forbid(instruction)
                    } else {
                        when (Enforcer(instruction).runFor(className)) {
                            Choice.FORBID -> forbid(instruction)
                            Choice.INIT_CLASSLOADER -> initClassLoader()
                            Choice.NEW_INSTANCE -> djvmInstance(instruction)
                            else -> Unit
                        }
                    }
            }
        }
    }

    private fun EmitterModule.forbid(instruction: MemberAccessInstruction) {
        throwRuleViolationError("Disallowed reference to API; ${formatFor(instruction)}")
        preventDefault()
    }

    private fun EmitterModule.initClassLoader() {
        invokeStatic(DJVM_NAME, "getSystemClassLoader", "()Ljava/lang/ClassLoader;")
        invokeSpecial(CLASSLOADER_NAME, CONSTRUCTOR_NAME, "(Ljava/lang/ClassLoader;)V")
        preventDefault()
    }

    private fun EmitterModule.djvmInstance(instruction: MemberAccessInstruction) {
        invokeVirtual(instruction.className, "djvmInstance", instruction.descriptor)
        preventDefault()
    }

    private fun isObjectMonitor(instruction: MemberAccessInstruction): Boolean
        = isObjectMonitor(instruction.memberName, instruction.descriptor)

    private enum class Choice {
        FORBID,
        INIT_CLASSLOADER,
        NEW_INSTANCE,
        PASS
    }

    private class Enforcer(private val instruction: MemberAccessInstruction) {
        private val isClassLoader: Boolean = instruction.className == CLASSLOADER_NAME
        private val isLoadClass: Boolean = instruction.memberName == "loadClass"

        private val hasClassReflection: Boolean get() = instruction.className == CLASS_NAME
            && instruction.descriptor.contains("Ljava/lang/reflect/")
            && instruction.memberName !in ALLOWED_GETTERS

        private val isNewInstance: Boolean get() = instruction.className == "java/lang/reflect/Constructor"
            && instruction.memberName == "newInstance"

        fun runFor(className: String): Choice = when {
            isClassLoader && instruction.memberName == CONSTRUCTOR_NAME -> if (instruction.descriptor == "()V") {
                Choice.INIT_CLASSLOADER
            } else {
                Choice.FORBID
            }

            // Are we allowed to invoke Constructor.newInstance()?
            isNewInstance -> forbidNewInstance(className)

            // Forbid reflection otherwise.
            hasClassReflection -> Choice.FORBID

            else -> allowLoadClass()
        }

        private fun forbidNewInstance(className: String): Choice = when(className) {
            in NEW_INSTANCE_CLASSES -> Choice.NEW_INSTANCE
            else -> Choice.FORBID
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
