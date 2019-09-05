package net.corda.djvm.serialization.serializers

import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.OpaqueBytesSubSequence
import net.corda.djvm.rewiring.SandboxClassLoader
import net.corda.djvm.serialization.deserializers.OpaqueBytesSubSequenceDeserializer
import net.corda.djvm.serialization.toSandboxAnyClass
import net.corda.serialization.internal.amqp.CustomSerializer
import net.corda.serialization.internal.amqp.SerializerFactory
import java.util.Collections.singleton
import java.util.function.BiFunction

class SandboxOpaqueBytesSubSequenceSerializer(
    classLoader: SandboxClassLoader,
    private val executor: BiFunction<in Any, in Any?, out Any?>,
    factory: SerializerFactory
) : CustomSerializer.Proxy<Any, Any>(
    clazz = classLoader.toSandboxAnyClass(OpaqueBytesSubSequence::class.java),
    proxyClass = classLoader.toSandboxAnyClass(OpaqueBytes::class.java),
    factory = factory
) {
    private val task = classLoader.toSandboxClass(OpaqueBytesSubSequenceDeserializer::class.java).newInstance()

    override val deserializationAliases: Set<Class<*>> = singleton(OpaqueBytesSubSequence::class.java)

    override fun toProxy(obj: Any): Any = abortReadOnly()

    override fun fromProxy(proxy: Any): Any {
        return executor.apply(task, proxy)!!
    }
}
