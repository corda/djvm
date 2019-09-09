package net.corda.djvm.serialization.serializers

import net.corda.djvm.rewiring.SandboxClassLoader
import net.corda.djvm.serialization.deserializers.CertPathDeserializer
import net.corda.djvm.serialization.toSandboxAnyClass
import net.corda.serialization.internal.amqp.CustomSerializer
import net.corda.serialization.internal.amqp.SerializerFactory
import net.corda.serialization.internal.amqp.custom.CertPathSerializer.CertPathProxy
import java.security.cert.CertPath
import java.util.Collections.singleton
import java.util.function.Function

class SandboxCertPathSerializer(
    classLoader: SandboxClassLoader,
    taskFactory: Function<in Any, out Function<in Any?, out Any?>>,
    factory: SerializerFactory
) : CustomSerializer.Proxy<Any, Any>(
    clazz = classLoader.toSandboxAnyClass(CertPath::class.java),
    proxyClass = classLoader.toSandboxAnyClass(CertPathProxy::class.java),
    factory = factory
) {
    private val task = classLoader.createTaskFor(taskFactory, CertPathDeserializer::class.java)

    override val deserializationAliases: Set<Class<*>> = singleton(CertPath::class.java)

    override fun toProxy(obj: Any): Any = abortReadOnly()

    override fun fromProxy(proxy: Any): Any {
        return task.apply(proxy)!!
    }
}
