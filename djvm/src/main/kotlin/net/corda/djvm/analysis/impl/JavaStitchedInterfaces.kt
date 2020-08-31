@file:JvmName("JavaStitchedInterfaces")
package net.corda.djvm.analysis.impl

import net.corda.djvm.analysis.AnalysisConfiguration.Companion.sandboxed
import net.corda.djvm.code.EmitterModule
import net.corda.djvm.references.Member
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.ACC_ABSTRACT
import org.objectweb.asm.Opcodes.ACC_BRIDGE
import org.objectweb.asm.Opcodes.ACC_PUBLIC
import org.objectweb.asm.Opcodes.ACC_SYNTHETIC

fun generateInterfaceBridgeMethods(): List<Member> = listOf(
    object : MethodBuilder(
        access = ACC_PUBLIC or ACC_SYNTHETIC or ACC_BRIDGE,
        className = sandboxed(CharSequence::class.java),
        memberName = "subSequence",
        descriptor = "(II)Ljava/lang/CharSequence;"
    ) {
        override fun writeBody(emitter: EmitterModule) = with(emitter) {
            pushObject(0)
            pushInteger(1)
            pushInteger(2)
            invokeInterface(className, memberName, "(II)L$className;")
            returnObject()
        }
    }.withBody()
     .build(),

    MethodBuilder(
        access = ACC_PUBLIC or ACC_ABSTRACT,
        className = sandboxed(CharSequence::class.java),
        memberName = "toString",
        descriptor = "()Ljava/lang/String;"
    ).build(),

    object : MethodBuilder(
        access = ACC_PUBLIC or ACC_SYNTHETIC or ACC_BRIDGE,
        className = sandboxed(Iterable::class.java),
        memberName = "iterator",
        descriptor = "()Ljava/util/Iterator;"
    ) {
        override fun writeBody(emitter: EmitterModule) = with(emitter) {
            val doStart = Label()
            lineNumber(0, doStart)
            pushObject(0)
            invokeInterface(className, memberName, "()Lsandbox/java/util/Iterator;")
            val doEnd = Label()
            lineNumber(1, doEnd)
            returnObject()
            newLocal(
                name = "this",
                descriptor = "Lsandbox/java/lang/Iterable;",
                // We are assuming that this interface is declared as "Iterable<T>".
                signature = "Lsandbox/java/lang/Iterable<TT;>;",
                start = doStart,
                end = doEnd,
                index = 0
            )
        }
    }.withBody()
     .build()
)
