package net.corda.djvm.serialization.serializers

import net.corda.djvm.rewiring.SandboxClassLoader
import net.corda.djvm.serialization.deserializers.ZoneIdDeserializer
import net.corda.djvm.serialization.toSandboxAnyClass
import net.corda.serialization.internal.amqp.CustomSerializer
import net.corda.serialization.internal.amqp.SerializerFactory
import net.corda.serialization.internal.amqp.custom.ZoneIdSerializer.ZoneIdProxy
import java.time.ZoneId
import java.util.Collections.singleton
import java.util.function.BiFunction

class SandboxZoneIdSerializer(
    classLoader: SandboxClassLoader,
    private val executor: BiFunction<in Any, in Any?, out Any?>,
    factory: SerializerFactory
) : CustomSerializer.Proxy<Any, Any>(
    clazz = classLoader.toSandboxAnyClass(ZoneId::class.java),
    proxyClass = classLoader.toSandboxAnyClass(ZoneIdProxy::class.java),
    factory = factory
) {
    private val task = classLoader.toSandboxClass(ZoneIdDeserializer::class.java).newInstance()

    override val revealSubclassesInSchema: Boolean = true

    override val deserializationAliases: Set<Class<*>> = singleton(ZoneId::class.java)

    override fun toProxy(obj: Any): Any = abortReadOnly()

    override fun fromProxy(proxy: Any): Any {
        return executor.apply(task, proxy)!!
    }
}
