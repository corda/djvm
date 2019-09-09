package net.corda.djvm.serialization.serializers

import net.corda.djvm.rewiring.SandboxClassLoader
import net.corda.djvm.serialization.deserializers.LocalDateDeserializer
import net.corda.djvm.serialization.toSandboxAnyClass
import net.corda.serialization.internal.amqp.CustomSerializer
import net.corda.serialization.internal.amqp.SerializerFactory
import net.corda.serialization.internal.amqp.custom.LocalDateSerializer.LocalDateProxy
import java.time.LocalDate
import java.util.Collections.singleton
import java.util.function.Function

class SandboxLocalDateSerializer(
    classLoader: SandboxClassLoader,
    taskFactory: Function<in Any, out Function<in Any?, out Any?>>,
    factory: SerializerFactory
) : CustomSerializer.Proxy<Any, Any>(
    clazz = classLoader.toSandboxAnyClass(LocalDate::class.java),
    proxyClass = classLoader.toSandboxAnyClass(LocalDateProxy::class.java),
    factory = factory
) {
    private val task = classLoader.createTaskFor(taskFactory, LocalDateDeserializer::class.java)

    override val deserializationAliases: Set<Class<*>> = singleton(LocalDate::class.java)

    override fun toProxy(obj: Any): Any = abortReadOnly()

    override fun fromProxy(proxy: Any): Any {
        return task.apply(proxy)!!
    }
}
