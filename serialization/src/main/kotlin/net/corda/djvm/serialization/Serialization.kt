@file:JvmName("Serialization")
package net.corda.djvm.serialization

import net.corda.core.serialization.SerializationContext
import net.corda.core.serialization.internal.SerializationEnvironment
import net.corda.djvm.rewiring.SandboxClassLoader
import net.corda.djvm.source.ClassSource
import net.corda.serialization.internal.GlobalTransientClassWhiteList
import net.corda.serialization.internal.SerializationContextImpl
import net.corda.serialization.internal.SerializationFactoryImpl
import net.corda.serialization.internal.amqp.amqpMagic
import net.corda.serialization.internal.amqp.createSerializerFactoryFactory

fun SandboxClassLoader.loadClassForSandbox(clazz: Class<*>): Class<Any> {
    @Suppress("unchecked_cast")
    return loadClassForSandbox(ClassSource.fromClassName(clazz.name)) as Class<Any>
}

fun createSandboxSerializationEnv(classLoader: SandboxClassLoader): SerializationEnvironment {
    val p2pContext: SerializationContext = SerializationContextImpl(
        preferredSerializationVersion = amqpMagic,
        deserializationClassLoader = DelegatingClassLoader(classLoader),
        whitelist = GlobalTransientClassWhiteList(SandboxExceptionsWhitelist()),
        properties = emptyMap(),
        objectReferencesEnabled = true,
        carpenterDisabled = true,
        useCase = SerializationContext.UseCase.P2P,
        encoding = null
    )

    val factory = SerializationFactoryImpl(mutableMapOf()).apply {
        registerScheme(SandboxAMQPSerializationScheme(classLoader, createSerializerFactoryFactory()))
    }
    return SerializationEnvironment.with(factory, p2pContext = p2pContext)
}
