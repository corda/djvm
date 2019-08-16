package net.corda.djvm.serialization.serializers

import net.corda.core.serialization.SerializationContext
import net.corda.djvm.rewiring.SandboxClassLoader
import net.corda.djvm.serialization.asTypeErasedProxy
import net.corda.djvm.serialization.createFingerprintProxy
import net.corda.djvm.serialization.deserializers.CreateMap
import net.corda.djvm.serialization.loadClassForSandbox
import net.corda.serialization.internal.amqp.*
import org.apache.qpid.proton.amqp.Symbol
import org.apache.qpid.proton.codec.Data
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.*
import java.util.function.BiFunction
import java.util.function.Function

class SandboxMapSerializer(
    classLoader: SandboxClassLoader,
    executor: BiFunction<in Any, in Any?, out Any?>,
    private val localFactory: LocalSerializerFactory
) : CustomSerializer.Implements<Any>(clazz = classLoader.loadClassForSandbox(Map::class.java)) {
    private val creator: Function<Array<Any>, out Any?>

    init {
        val createTask: Any = classLoader.loadClassForSandbox(CreateMap::class.java).newInstance()
        creator = Function { inputs ->
            executor.apply(createTask, inputs)
        }
    }

    // The order matters here - the first match should be the most specific one.
    // Kotlin preserves the ordering for us by associating into a LinkedHashMap.
    private val supportedTypes: Map<Class<Any>, Class<out Map<*, *>>> = listOf(
        NavigableMap::class.java,
        SortedMap::class.java,
        Map::class.java
    ).associateBy {
        classLoader.loadClassForSandbox(it)
    }

    private fun getBestMatchFor(type: Class<Any>): Map.Entry<Class<Any>, Class<out Map<*, *>>>
        = supportedTypes.entries.first { it.key.isAssignableFrom(type) }

    override val schemaForDocumentation: Schema = Schema(emptyList())

    override fun specialiseFor(declaredType: Type): AMQPSerializer<Any> {
        val parameterizedType = when (declaredType) {
            is ParameterizedType -> declaredType
            is Class<*> -> declaredType.asTypeErasedProxy(parameterCount = 2)
            else -> throw AMQPNotSerializableException(declaredType, "type=${declaredType.typeName} is not serializable")
        }

        @Suppress("unchecked_cast")
        val rawType = parameterizedType.rawType as Class<Any>
        return ConcreteMapSerializer(parameterizedType, getBestMatchFor(rawType), creator, localFactory)
    }

    override fun readObject(obj: Any, schemas: SerializationSchemas, input: DeserializationInput, context: SerializationContext): Any {
        throw UnsupportedOperationException("Factory only")
    }

    override fun writeDescribedObject(obj: Any, data: Data, type: Type, output: SerializationOutput, context: SerializationContext) {
        throw UnsupportedOperationException("Factory Only")
    }
}

private class ConcreteMapSerializer(
    private val declaredType: ParameterizedType,
    private val matchingType: Map.Entry<Class<Any>, Class<out Map<*, *>>>,
    private val creator: Function<Array<Any>, out Any?>,
    factory: LocalSerializerFactory
) : AMQPSerializer<Any> {
    override val type: ParameterizedType = declaredType

    override val typeDescriptor: Symbol by lazy {
        factory.createDescriptor(
            type.createFingerprintProxy(matchingType.value)
        )
    }

    override fun readObject(
        obj: Any,
        schemas: SerializationSchemas,
        input: DeserializationInput,
        context: SerializationContext
    ): Any {
        val inboundKeyType = declaredType.actualTypeArguments[0]
        val inboundValueType = declaredType.actualTypeArguments[1]
        return ifThrowsAppend({ declaredType.typeName }) {
            val entries = (obj as Map<*, *>).map {
                arrayOf(
                    input.readObjectOrNull(redescribe(it.key, inboundKeyType), schemas, inboundKeyType, context),
                    input.readObjectOrNull(redescribe(it.value, inboundValueType), schemas, inboundValueType, context)
                )
            }.toTypedArray()
            creator.apply(arrayOf(matchingType.key, entries))!!
        }
    }

    override fun writeClassInfo(output: SerializationOutput) {
        throw UnsupportedOperationException("Read Only")
    }

    override fun writeObject(obj: Any, data: Data, type: Type, output: SerializationOutput, context: SerializationContext, debugIndent: Int) {
        throw UnsupportedOperationException("Read Only!")
    }
}
