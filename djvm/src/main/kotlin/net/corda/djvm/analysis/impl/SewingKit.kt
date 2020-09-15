@file:JvmName("SewingKit")
package net.corda.djvm.analysis.impl

import net.corda.djvm.CordaInternal
import net.corda.djvm.code.impl.EmitterModuleImpl
import net.corda.djvm.code.impl.FROM_DJVM
import net.corda.djvm.code.impl.toMethodBody
import net.corda.djvm.references.Member
import net.corda.djvm.references.MethodBody
import org.objectweb.asm.Opcodes.ACC_BRIDGE
import org.objectweb.asm.Opcodes.ACC_FINAL
import org.objectweb.asm.Opcodes.ACC_PROTECTED
import org.objectweb.asm.Opcodes.ACC_SYNTHETIC
import java.util.Collections.unmodifiableList
import java.util.Collections.unmodifiableSet

@CordaInternal
open class MethodBuilder(
    protected val access: Int,
    protected val className: String,
    protected val memberName: String,
    protected val descriptor: String,
    protected val signature: String = "",
    protected val exceptions: Set<String> = emptySet()
) {
    private val bodies = mutableListOf<MethodBody>()

    protected open fun writeBody(emitter: EmitterModuleImpl) {}

    fun withBody(body: MethodBody): MethodBuilder {
        bodies.add(body)
        return this
    }

    fun withBody() = withBody(toMethodBody(::writeBody))

    fun build() = Member(
        access = access,
        className = className,
        memberName = memberName,
        descriptor = descriptor,
        genericsDetails = signature,
        exceptions = unmodifiableSet(exceptions),
        body = unmodifiableList(bodies)
    )
}

@CordaInternal
abstract class FromDJVMBuilder(
    protected val className: String,
    private val bridgeDescriptor: String,
    signature: String
) {
    constructor(className: String, bridgeDescriptor: String)
        : this(className, bridgeDescriptor, "")

    private val builder = MethodBuilder(
        access = ACC_FINAL or ACC_PROTECTED,
        className = className,
        memberName = FROM_DJVM,
        descriptor = bridgeDescriptor,
        signature = signature
    )

    protected abstract fun writeBody(emitter: EmitterModuleImpl)

    fun build(): List<Member> = listOf(
        @Suppress("unchecked_cast")
        builder.withBody(toMethodBody(::writeBody)).build(),
        object : MethodBuilder(
            access = ACC_BRIDGE or ACC_SYNTHETIC or ACC_PROTECTED,
            className = className,
            memberName = FROM_DJVM,
            descriptor = "()Ljava/lang/Object;"
        ) {
            override fun writeBody(emitter: EmitterModuleImpl) = with(emitter) {
                pushObject(0)
                invokeVirtual(className, memberName, bridgeDescriptor)
                returnObject()
            }
        }.withBody()
         .build()
    )
}
