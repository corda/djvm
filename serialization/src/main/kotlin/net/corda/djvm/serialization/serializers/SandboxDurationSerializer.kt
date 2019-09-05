package net.corda.djvm.serialization.serializers

import net.corda.djvm.rewiring.SandboxClassLoader
import net.corda.djvm.serialization.deserializers.DurationDeserializer
import net.corda.djvm.serialization.toSandboxAnyClass
import net.corda.serialization.internal.amqp.CustomSerializer
import net.corda.serialization.internal.amqp.SerializerFactory
import net.corda.serialization.internal.amqp.custom.DurationSerializer.DurationProxy
import java.time.Duration
import java.util.Collections.singleton
import java.util.function.BiFunction

class SandboxDurationSerializer(
    classLoader: SandboxClassLoader,
    private val executor: BiFunction<in Any, in Any?, out Any?>,
    factory: SerializerFactory
) : CustomSerializer.Proxy<Any, Any>(
    clazz = classLoader.toSandboxAnyClass(Duration::class.java),
    proxyClass = classLoader.toSandboxAnyClass(DurationProxy::class.java),
    factory = factory
) {
    private val task = classLoader.toSandboxClass(DurationDeserializer::class.java).newInstance()

    override val deserializationAliases: Set<Class<*>> = singleton(Duration::class.java)

    override fun toProxy(obj: Any): Any = abortReadOnly()

    override fun fromProxy(proxy: Any): Any {
        return executor.apply(task, proxy)!!
    }
}
