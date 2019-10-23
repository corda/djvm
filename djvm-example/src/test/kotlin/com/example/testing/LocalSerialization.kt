package com.example.testing

import net.corda.core.serialization.SerializationContext
import net.corda.core.serialization.SerializationContext.UseCase.P2P
import net.corda.core.serialization.SerializationCustomSerializer
import net.corda.core.serialization.SerializationWhitelist
import net.corda.core.serialization.internal.SerializationEnvironment
import net.corda.core.serialization.internal._contextSerializationEnv
import net.corda.serialization.internal.BuiltInExceptionsWhitelist
import net.corda.serialization.internal.CordaSerializationMagic
import net.corda.serialization.internal.GlobalTransientClassWhiteList
import net.corda.serialization.internal.SerializationContextImpl
import net.corda.serialization.internal.SerializationFactoryImpl
import net.corda.serialization.internal.amqp.AbstractAMQPSerializationScheme
import net.corda.serialization.internal.amqp.AccessOrderLinkedHashMap
import net.corda.serialization.internal.amqp.SerializationFactoryCacheKey
import net.corda.serialization.internal.amqp.SerializerFactory
import net.corda.serialization.internal.amqp.amqpMagic
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class LocalSerialization : BeforeEachCallback, AfterEachCallback {
    private companion object {
        private val AMQP_P2P_CONTEXT = SerializationContextImpl(
            amqpMagic,
            LocalSerialization::class.java.classLoader,
            GlobalTransientClassWhiteList(BuiltInExceptionsWhitelist()),
            emptyMap(),
            true,
            P2P,
            null
        )
    }

    override fun beforeEach(context: ExtensionContext) {
        _contextSerializationEnv.set(createTestSerializationEnv())
    }

    override fun afterEach(context: ExtensionContext) {
        _contextSerializationEnv.set(null)
    }

    private fun createTestSerializationEnv(): SerializationEnvironment {
        val factory = SerializationFactoryImpl(mutableMapOf()).apply {
            registerScheme(AMQPSerializationScheme(emptySet(), emptySet(), AccessOrderLinkedHashMap(128)))
        }
        return SerializationEnvironment.with(factory, p2pContext = AMQP_P2P_CONTEXT)
    }

    private class AMQPSerializationScheme(
        cordappCustomSerializers: Set<SerializationCustomSerializer<*, *>>,
        cordappSerializationWhitelists: Set<SerializationWhitelist>,
        serializerFactoriesForContexts: AccessOrderLinkedHashMap<SerializationFactoryCacheKey, SerializerFactory>
    ) : AbstractAMQPSerializationScheme(cordappCustomSerializers, cordappSerializationWhitelists, serializerFactoriesForContexts) {
        override fun rpcServerSerializerFactory(context: SerializationContext): SerializerFactory {
            throw UnsupportedOperationException()
        }

        override fun rpcClientSerializerFactory(context: SerializationContext): SerializerFactory {
            throw UnsupportedOperationException()
        }

        override fun canDeserializeVersion(magic: CordaSerializationMagic, target: SerializationContext.UseCase): Boolean {
            return canDeserializeVersion(magic) && target == P2P
        }
    }
}
