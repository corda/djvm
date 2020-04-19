package net.corda.djvm.rules.implementation

import net.corda.djvm.code.CLASS_NAME
import net.corda.djvm.code.EMIT_AFTER_INVOKE
import net.corda.djvm.code.Emitter
import net.corda.djvm.code.EmitterContext
import net.corda.djvm.code.Instruction
import net.corda.djvm.code.OBJECT_NAME
import net.corda.djvm.code.instructions.MemberAccessInstruction
import org.objectweb.asm.Opcodes.INVOKESPECIAL
import org.objectweb.asm.Opcodes.INVOKESTATIC
import org.objectweb.asm.Opcodes.INVOKEVIRTUAL

/**
 * These factory functions need special handling:
 *   [java.util.concurrent.atomic.AtomicIntegerFieldUpdater.newUpdater]
 *   [java.util.concurrent.atomic.AtomicLongFieldUpdater.newUpdater]
 *   [java.util.concurrent.atomic.AtomicReferenceFieldUpdater.newUpdater]
 *
 * The AtomicFieldUpdater classes are too complex to be worth replacing with
 * template classes, so we create actual AtomicFieldUpdater instances via
 * their factory functions and then wrap them for use inside the sandbox.
 *
 * The factory functions invoke [sun.reflect.Reflection.getCallerClass] internally,
 * which means that we must continue to invoke them where they are.
 *
 * Technically [sun.reflect.Reflection.getCallerClass] is like passing the result of
 * [javaClass] from the caller, except that the caller may also be static.
 */
object ReturnTypeWrapper : Emitter {
    private val ATOMIC_FIELD_UPDATER = "^java/util/concurrent/atomic/Atomic(Integer|Long|Reference)FieldUpdater\$".toRegex()
    private val EXCLUDE_STRING = setOf(CLASS_NAME, OBJECT_NAME)

    /**
     * Ensure that this emitter executes after all of the emitters which
     * modify the method invocations themselves.
     */
    override val priority: Int = EMIT_AFTER_INVOKE

    override fun emit(context: EmitterContext, instruction: Instruction) = context.emit {
        if (instruction is MemberAccessInstruction && context.whitelist.matches(instruction.reference)) {
            fun invokeMethod() = when (instruction.operation) {
                INVOKEVIRTUAL -> invokeVirtual(instruction.className, instruction.memberName, instruction.descriptor)
                INVOKESTATIC -> invokeStatic(instruction.className, instruction.memberName, instruction.descriptor)
                INVOKESPECIAL -> invokeSpecial(instruction.className, instruction.memberName, instruction.descriptor)
                else -> Unit
            }

            if (hasStringReturnType(instruction) && instruction.className !in EXCLUDE_STRING) {
                preventDefault()
                invokeMethod()
                invokeStatic(
                    owner = "sandbox/java/lang/String",
                    name = "toDJVM",
                    descriptor = "(Ljava/lang/String;)Lsandbox/java/lang/String;"
                )
            } else if (isAtomicFieldUpdaterFactory(instruction)) {
                preventDefault()
                invokeMethod()
                invokeStatic(
                    owner = "sandbox/java/util/concurrent/atomic/DJVM",
                    name = "toDJVM",
                    descriptor = "(L${instruction.className};)Lsandbox/${instruction.className};"
                )
            }
        }
    }

    private fun hasStringReturnType(method: MemberAccessInstruction) = method.descriptor.endsWith(")Ljava/lang/String;")
    private fun isAtomicFieldUpdaterFactory(method: MemberAccessInstruction)
                    = method.memberName == "newUpdater" && method.className.matches(ATOMIC_FIELD_UPDATER)
}
