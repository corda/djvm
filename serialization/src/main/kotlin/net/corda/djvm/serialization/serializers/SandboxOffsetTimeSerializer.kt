package net.corda.djvm.serialization.serializers

import net.corda.djvm.rewiring.SandboxClassLoader
import net.corda.djvm.serialization.deserializers.OffsetTimeDeserializer
import net.corda.djvm.serialization.toSandboxAnyClass
import net.corda.serialization.internal.amqp.CustomSerializer
import net.corda.serialization.internal.amqp.SerializerFactory
import net.corda.serialization.internal.amqp.custom.OffsetTimeSerializer.OffsetTimeProxy
import java.time.OffsetTime
import java.util.Collections.singleton
import java.util.function.BiFunction

class SandboxOffsetTimeSerializer(
    classLoader: SandboxClassLoader,
    private val executor: BiFunction<in Any, in Any?, out Any?>,
    factory: SerializerFactory
) : CustomSerializer.Proxy<Any, Any>(
    clazz = classLoader.toSandboxAnyClass(OffsetTime::class.java),
    proxyClass = classLoader.toSandboxAnyClass(OffsetTimeProxy::class.java),
    factory = factory
) {
    private val task = classLoader.toSandboxClass(OffsetTimeDeserializer::class.java).newInstance()

    override val deserializationAliases: Set<Class<*>> = singleton(OffsetTime::class.java)

    override fun toProxy(obj: Any): Any = abortReadOnly()

    override fun fromProxy(proxy: Any): Any {
        return executor.apply(task, proxy)!!
    }
}
