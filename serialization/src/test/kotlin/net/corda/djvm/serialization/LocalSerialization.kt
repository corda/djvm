package net.corda.djvm.serialization

import net.corda.core.serialization.SerializationContext
import net.corda.core.serialization.SerializationContext.UseCase
import net.corda.core.serialization.SerializationCustomSerializer
import net.corda.core.serialization.SerializationWhitelist
import net.corda.core.serialization.internal.SerializationEnvironment
import net.corda.core.serialization.internal._contextSerializationEnv
import net.corda.serialization.internal.*
import net.corda.serialization.internal.amqp.*
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
            UseCase.P2P,
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
        return SerializationEnvironment.with(factory, AMQP_P2P_CONTEXT)
    }

    private class AMQPSerializationScheme(
        customSerializers: Set<SerializationCustomSerializer<*, *>>,
        serializationWhitelists: Set<SerializationWhitelist>,
        serializerFactoriesForContexts: AccessOrderLinkedHashMap<SerializationFactoryCacheKey, SerializerFactory>
    ) : AbstractAMQPSerializationScheme(customSerializers, serializationWhitelists, serializerFactoriesForContexts) {
        override fun rpcServerSerializerFactory(context: SerializationContext): SerializerFactory {
            throw UnsupportedOperationException()
        }

        override fun rpcClientSerializerFactory(context: SerializationContext): SerializerFactory {
            throw UnsupportedOperationException()
        }

        override fun canDeserializeVersion(magic: CordaSerializationMagic, target: UseCase): Boolean {
            return canDeserializeVersion(magic) && target == UseCase.P2P
        }
    }
}
