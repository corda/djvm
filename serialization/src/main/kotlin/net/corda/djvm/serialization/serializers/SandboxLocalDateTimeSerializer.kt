package net.corda.djvm.serialization.serializers

import net.corda.djvm.rewiring.SandboxClassLoader
import net.corda.djvm.serialization.deserializers.LocalDateTimeDeserializer
import net.corda.djvm.serialization.toSandboxAnyClass
import net.corda.serialization.internal.amqp.CustomSerializer
import net.corda.serialization.internal.amqp.SerializerFactory
import net.corda.serialization.internal.amqp.custom.LocalDateTimeSerializer.LocalDateTimeProxy
import java.time.LocalDateTime
import java.util.Collections.singleton
import java.util.function.BiFunction

class SandboxLocalDateTimeSerializer(
    classLoader: SandboxClassLoader,
    private val executor: BiFunction<in Any, in Any?, out Any?>,
    factory: SerializerFactory
) : CustomSerializer.Proxy<Any, Any>(
    clazz = classLoader.toSandboxAnyClass(LocalDateTime::class.java),
    proxyClass = classLoader.toSandboxAnyClass(LocalDateTimeProxy::class.java),
    factory = factory
) {
    private val task = classLoader.toSandboxClass(LocalDateTimeDeserializer::class.java).newInstance()

    override val deserializationAliases: Set<Class<*>> = singleton(LocalDateTime::class.java)

    override fun toProxy(obj: Any): Any = abortReadOnly()

    override fun fromProxy(proxy: Any): Any {
        return executor.apply(task, proxy)!!
    }
}
