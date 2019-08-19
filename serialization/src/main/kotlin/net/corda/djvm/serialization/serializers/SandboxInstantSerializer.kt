package net.corda.djvm.serialization.serializers

import net.corda.djvm.rewiring.SandboxClassLoader
import net.corda.djvm.serialization.deserializers.InstantDeserializer
import net.corda.djvm.serialization.loadClassForSandbox
import net.corda.serialization.internal.amqp.CustomSerializer
import net.corda.serialization.internal.amqp.SerializerFactory
import net.corda.serialization.internal.amqp.custom.InstantSerializer
import java.time.Instant
import java.util.Collections.singleton
import java.util.function.BiFunction

class SandboxInstantSerializer(classLoader: SandboxClassLoader,
                               private val executor: BiFunction<in Any, in Any?, out Any?>,
                               factory: SerializerFactory)
 : CustomSerializer.Proxy<Any, Any>(
    clazz = classLoader.loadClassForSandbox(Instant::class.java),
    proxyClass = classLoader.loadClassForSandbox(InstantSerializer.InstantProxy::class.java),
    factory = factory
) {
    private val task = classLoader.loadClassForSandbox(InstantDeserializer::class.java).newInstance()

    override val deserializationAliases: Set<Class<*>> = singleton(Instant::class.java)

    override fun toProxy(obj: Any): Any = throw UnsupportedOperationException("Read Only!")

    override fun fromProxy(proxy: Any): Any {
        return executor.apply(task, proxy)!!
    }
}
