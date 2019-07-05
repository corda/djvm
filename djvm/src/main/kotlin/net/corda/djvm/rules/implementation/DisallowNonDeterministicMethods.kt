package net.corda.djvm.rules.implementation

import net.corda.djvm.code.*
import net.corda.djvm.code.instructions.MemberAccessInstruction
import org.objectweb.asm.Opcodes.*

/**
 * Some non-deterministic APIs belong to pinned classes and so cannot be stubbed out.
 * Replace their invocations with safe alternatives, e.g. throwing an exception.
 */
object DisallowNonDeterministicMethods : Emitter {

    private const val CHARSET_PACKAGE = "sun/nio/cs/"
    private const val SUN_SECURITY_PROVIDERS = "sun/security/jca/Provider"
    private val MONITOR_METHODS = setOf("notify", "notifyAll", "wait")
    private val CLASSLOADING_METHODS = setOf("defineClass", "findClass")

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
                        Choice.INIT_CLASSLOADER -> initClassLoader()
                        Choice.GET_PARENT -> returnNull(POP)
                        Choice.NO_RESOURCE -> returnNull(POP2)
                        Choice.EMPTY_RESOURCES -> emptyResources(POP2)
                        else -> Unit
                    }
                }

                INVOKESTATIC -> if (instruction.className == "java/lang/ClassLoader") {
                    when {
                        instruction.memberName == "getSystemClassLoader" -> {
                            invokeStatic(DJVM_NAME, instruction.memberName, instruction.descriptor)
                            preventDefault()
                        }
                        instruction.memberName == "getSystemResources" -> emptyResources(POP)
                        instruction.memberName.startsWith("getSystemResource") -> returnNull(POP)
                        else -> forbid(instruction)
                    }
                }
            }
        }
    }

    private fun EmitterModule.forbid(instruction: MemberAccessInstruction) {
        throwRuleViolationError("Disallowed reference to API; ${formatFor(instruction)}")
        preventDefault()
    }

    private fun EmitterModule.loadClass() {
        invokeStatic(DJVM_NAME, "loadClass", "(Ljava/lang/ClassLoader;Ljava/lang/String;)Ljava/lang/Class;")
        preventDefault()
    }

    private fun EmitterModule.initClassLoader() {
        invokeStatic(DJVM_NAME, "getSystemClassLoader", "()Ljava/lang/ClassLoader;")
        invokeSpecial("java/lang/ClassLoader", "<init>", "(Ljava/lang/ClassLoader;)V")
        preventDefault()
    }

    private fun EmitterModule.returnNull(popCode: Int) {
        instruction(popCode)
        pushNull()
        preventDefault()
    }

    private fun EmitterModule.emptyResources(popCode: Int) {
        instruction(popCode)
        invokeStatic("sandbox/java/util/Collections", "emptyEnumeration", "()Lsandbox/java/util/Enumeration;")
        preventDefault()
    }

    private fun isObjectMonitor(instruction: MemberAccessInstruction): Boolean =
        (instruction.descriptor == "()V" && instruction.memberName in MONITOR_METHODS)
            || (instruction.memberName == "wait" && (instruction.descriptor == "(J)V" || instruction.descriptor == "(JI)V"))

    private enum class Choice {
        FORBID,
        LOAD_CLASS,
        INIT_CLASSLOADER,
        GET_PARENT,
        NO_RESOURCE,
        EMPTY_RESOURCES,
        PASS
    }

    private class Enforcer(private val instruction: MemberAccessInstruction) {
        private val isClassLoader: Boolean = instruction.className == "java/lang/ClassLoader"
        private val isClass: Boolean = instruction.className == "java/lang/Class"
        private val hasClassReflection: Boolean = isClass && instruction.descriptor.contains("Ljava/lang/reflect/")
        private val isLoadClass: Boolean = instruction.memberName == "loadClass"

        fun runFor(className: String): Choice = when {
            isClassLoader && instruction.memberName == "<init>" -> if (instruction.descriptor == "()V") {
                Choice.INIT_CLASSLOADER
            } else {
                Choice.FORBID
            }
            isClassLoader && instruction.memberName == "getParent" -> Choice.GET_PARENT
            isClassLoader && instruction.memberName == "getResources" -> Choice.EMPTY_RESOURCES
            isClassLoader && instruction.memberName.startsWith("getResource") -> Choice.NO_RESOURCE

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
            hasClassReflection || hasAnyClassLoading() -> Choice.FORBID

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