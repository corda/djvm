package net.corda.djvm.serialization.serializers

import net.corda.djvm.rewiring.SandboxClassLoader
import net.corda.djvm.serialization.deserializers.BitSetDeserializer
import net.corda.djvm.serialization.toSandboxAnyClass
import net.corda.serialization.internal.amqp.CustomSerializer
import net.corda.serialization.internal.amqp.SerializerFactory
import net.corda.serialization.internal.amqp.custom.BitSetSerializer.BitSetProxy
import java.util.*
import java.util.Collections.singleton
import java.util.function.BiFunction

class SandboxBitSetSerializer(
    classLoader: SandboxClassLoader,
    private val executor: BiFunction<in Any, in Any?, out Any?>,
    factory: SerializerFactory
) : CustomSerializer.Proxy<Any, Any>(
    clazz = classLoader.toSandboxAnyClass(BitSet::class.java),
    proxyClass = classLoader.toSandboxAnyClass(BitSetProxy::class.java),
    factory = factory
) {
    private val task = classLoader.toSandboxClass(BitSetDeserializer::class.java).newInstance()

    override val deserializationAliases: Set<Class<*>> = singleton(BitSet::class.java)

    override fun toProxy(obj: Any): Any = abortReadOnly()

    override fun fromProxy(proxy: Any): Any {
        return executor.apply(task, proxy)!!
    }
}
