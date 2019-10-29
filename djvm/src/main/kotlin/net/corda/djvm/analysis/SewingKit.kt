@file:JvmName("SewingKit")
package net.corda.djvm.analysis

import net.corda.djvm.CordaInternal
import net.corda.djvm.code.EmitterModule
import net.corda.djvm.references.Member
import net.corda.djvm.references.MethodBody
import org.objectweb.asm.Opcodes.ACC_BRIDGE
import org.objectweb.asm.Opcodes.ACC_FINAL
import org.objectweb.asm.Opcodes.ACC_PROTECTED
import org.objectweb.asm.Opcodes.ACC_SYNTHETIC

const val FROM_DJVM = "fromDJVM"

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

    protected open fun writeBody(emitter: EmitterModule) {}

    fun withBody(body: MethodBody): MethodBuilder {
        bodies.add(body)
        return this
    }

    fun withBody() = withBody(::writeBody)

    fun build() = Member(
        access = access,
        className = className,
        memberName = memberName,
        descriptor = descriptor,
        genericsDetails = signature,
        exceptions = exceptions.toMutableSet(),
        body = bodies
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

    protected abstract fun writeBody(emitter: EmitterModule)

    fun build(): List<Member> = listOf(
        builder.withBody(::writeBody).build(),
        object : MethodBuilder(
            access = ACC_BRIDGE or ACC_SYNTHETIC or ACC_PROTECTED,
            className = className,
            memberName = FROM_DJVM,
            descriptor = "()Ljava/lang/Object;"
        ) {
            override fun writeBody(emitter: EmitterModule) = with(emitter) {
                pushObject(0)
                invokeVirtual(className, memberName, bridgeDescriptor)
                returnObject()
            }
        }.withBody()
         .build()
    )
}
