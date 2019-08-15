@file:JvmName("Serialization")
package net.corda.djvm.serialization

import net.corda.core.serialization.SerializationContext
import net.corda.core.serialization.SerializationContext.UseCase
import net.corda.core.serialization.internal.SerializationEnvironment
import net.corda.djvm.rewiring.SandboxClassLoader
import net.corda.djvm.source.ClassSource
import net.corda.serialization.internal.GlobalTransientClassWhiteList
import net.corda.serialization.internal.SerializationContextImpl
import net.corda.serialization.internal.SerializationFactoryImpl
import net.corda.serialization.internal.amqp.amqpMagic
import net.corda.serialization.internal.amqp.createSerializerFactoryFactory
import org.jboss.logging.Param
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

fun SandboxClassLoader.loadClassForSandbox(clazz: Class<*>): Class<Any> {
    @Suppress("unchecked_cast")
    return loadClassForSandbox(ClassSource.fromClassName(clazz.name)) as Class<Any>
}

fun createSandboxSerializationEnv(classLoader: SandboxClassLoader): SerializationEnvironment {
    val p2pContext: SerializationContext = SerializationContextImpl(
        preferredSerializationVersion = amqpMagic,
        deserializationClassLoader = DelegatingClassLoader(classLoader),
        whitelist = GlobalTransientClassWhiteList(SandboxWhitelist()),
        properties = emptyMap(),
        objectReferencesEnabled = true,
        carpenterDisabled = true,
        useCase = UseCase.P2P,
        encoding = null
    )

    val factory = SerializationFactoryImpl(mutableMapOf()).apply {
        registerScheme(SandboxAMQPSerializationScheme(classLoader, createSerializerFactoryFactory()))
    }
    return SerializationEnvironment.with(factory, p2pContext = p2pContext)
}

fun ParameterizedType.createFingerprintProxy(proxyType: Type): ParameterizedType = ParameterizedTypeProxy(
    rawType = proxyType,
    ownerType = ownerType,
    actualTypeArguments = actualTypeArguments
)

fun Class<*>.asTypeErasedProxy(parameterCount: Int): ParameterizedType = ParameterizedTypeProxy(
    rawType = this,
    ownerType = null,
    actualTypeArguments = Array(parameterCount) { Any::class.java }
)

private class ParameterizedTypeProxy(
    private val rawType: Type,
    private val ownerType: Type?,
    private val actualTypeArguments: Array<Type>
) : ParameterizedType {
    override fun getRawType() = rawType
    override fun getOwnerType() = ownerType
    override fun getActualTypeArguments() = actualTypeArguments
}
