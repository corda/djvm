package net.corda.djvm.rules.implementation

import net.corda.djvm.code.*
import net.corda.djvm.code.instructions.MemberAccessInstruction
import org.objectweb.asm.Opcodes.*

/**
 * Some non-deterministic APIs belong to pinned classes and so cannot be stubbed out.
 * Replace their invocations with safe alternatives, e.g. throwing an exception.
 */
object DisallowNonDeterministicMethods : Emitter {

    private val MONITOR_METHODS = setOf("notify", "notifyAll", "wait")
    private val CLASSLOADING_METHODS = setOf("defineClass", "findClass")
    private val REFLECTING_CLASSES = setOf(
        "sun/security/x509/CertificateExtensions",
        "sun/security/x509/CRLExtensions",
        "sun/security/x509/OtherName"
    )

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
                        Choice.GET_RESOURCE -> loadResource()
                        Choice.GET_RESOURCES -> loadResources()
                        Choice.RESOURCE_STREAM -> loadResourceStream()
                        else -> Unit
                    }
                }

                INVOKESTATIC -> if (instruction.className == "java/lang/ClassLoader") {
                    when {
                        instruction.memberName == "getSystemClassLoader" -> {
                            invokeStatic(DJVM_NAME, instruction.memberName, instruction.descriptor)
                            preventDefault()
                        }
                        instruction.memberName == "getSystemResourceAsStream" -> {
                            invokeStatic(DJVM_NAME, instruction.memberName, "(Ljava/lang/String;)Lsandbox/java/io/InputStream;")
                            preventDefault()
                        }
                        instruction.memberName == "getSystemResources" -> {
                            invokeStatic(DJVM_NAME, instruction.memberName, "(Ljava/lang/String;)Lsandbox/java/util/Enumeration;")
                            preventDefault()
                        }
                        instruction.memberName == "getSystemResource" -> {
                            invokeStatic(DJVM_NAME, instruction.memberName, "(Ljava/lang/String;)Lsandbox/java/net/URL;")
                            preventDefault()
                        }
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
        invokeSpecial("java/lang/ClassLoader", CONSTRUCTOR_NAME, "(Ljava/lang/ClassLoader;)V")
        preventDefault()
    }

    private fun EmitterModule.returnNull(popCode: Int) {
        instruction(popCode)
        pushNull()
        preventDefault()
    }

    private fun EmitterModule.loadResourceStream() {
        invokeStatic(
            owner = DJVM_NAME,
            name = "getResourceAsStream",
            descriptor = "(Ljava/lang/ClassLoader;Ljava/lang/String;)Lsandbox/java/io/InputStream;"
        )
        preventDefault()
    }

    private fun EmitterModule.loadResource() {
        invokeStatic(
            owner = DJVM_NAME,
            name = "getResource",
            descriptor = "(Ljava/lang/ClassLoader;Ljava/lang/String;)Lsandbox/java/net/URL;"
        )
        preventDefault()
    }

    private fun EmitterModule.loadResources() {
        invokeStatic(
            owner = DJVM_NAME,
            name = "getResources",
            descriptor = "(Ljava/lang/ClassLoader;Ljava/lang/String;)Lsandbox/java/util/Enumeration;"
        )
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
        GET_RESOURCE,
        GET_RESOURCES,
        RESOURCE_STREAM,
        PASS
    }

    private class Enforcer(private val instruction: MemberAccessInstruction) {
        private val isClassLoader: Boolean = instruction.className == "java/lang/ClassLoader"
        private val isClass: Boolean = instruction.className == "java/lang/Class"
        private val hasClassReflection: Boolean = isClass && instruction.descriptor.contains("Ljava/lang/reflect/")
        private val isLoadClass: Boolean = instruction.memberName == "loadClass"

        fun runFor(className: String): Choice = when {
                isClassLoader && instruction.memberName == CONSTRUCTOR_NAME -> if (instruction.descriptor == "()V") {
                    Choice.INIT_CLASSLOADER
                } else {
                    Choice.FORBID
                }
            isClassLoader && instruction.memberName == "getParent" -> Choice.GET_PARENT
            isClassLoader && instruction.memberName == "getResources" -> Choice.GET_RESOURCES
            isClassLoader && instruction.memberName == "getResourceAsStream" -> Choice.RESOURCE_STREAM
            isClassLoader && instruction.memberName == "getResource" -> Choice.GET_RESOURCE

            className == "java/security/Provider\$Service" -> allowLoadClass()

            // Forbid reflection otherwise.
            hasClassReflection -> forbidReflection(className)

            else -> allowLoadClass()
        }

        private fun forbidReflection(className: String): Choice = when(className) {
            in REFLECTING_CLASSES -> Choice.PASS
            else -> Choice.FORBID
        }

        private fun allowLoadClass(): Choice = when {
            !isClassLoader -> Choice.PASS
            isLoadClass -> when (instruction.descriptor) {
                "(Ljava/lang/String;)Ljava/lang/Class;" -> Choice.LOAD_CLASS
                else -> Choice.FORBID
            }
            instruction.memberName in CLASSLOADING_METHODS -> Choice.FORBID
            else -> Choice.PASS
        }
    }
}
