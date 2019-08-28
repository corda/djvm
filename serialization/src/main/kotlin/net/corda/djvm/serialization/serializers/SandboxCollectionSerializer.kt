package net.corda.djvm.serialization.serializers

import net.corda.core.serialization.SerializationContext
import net.corda.core.utilities.NonEmptySet
import net.corda.djvm.rewiring.SandboxClassLoader
import net.corda.djvm.serialization.deserializers.CreateCollection
import net.corda.djvm.serialization.loadClassForSandbox
import net.corda.serialization.internal.amqp.*
import net.corda.serialization.internal.model.LocalTypeInformation
import net.corda.serialization.internal.model.TypeIdentifier
import org.apache.qpid.proton.amqp.Symbol
import org.apache.qpid.proton.codec.Data
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.*
import java.util.function.BiFunction
import java.util.function.Function

class SandboxCollectionSerializer(
    classLoader: SandboxClassLoader,
    executor: BiFunction<in Any, in Any?, out Any?>,
    private val localFactory: LocalSerializerFactory
) : CustomSerializer.Implements<Any>(clazz = classLoader.loadClassForSandbox(Collection::class.java)) {
    private val creator: Function<Array<Any>, out Any?>

    init {
        val createTask = classLoader.loadClassForSandbox(CreateCollection::class.java).newInstance()
        creator = Function { inputs ->
            executor.apply(createTask, inputs)
        }
    }

    private val unsupportedTypes: Set<Class<Any>> = listOf(
        EnumSet::class.java
    ).map {
        classLoader.loadClassForSandbox(it)
    }.toSet()

    // The order matters here - the first match should be the most specific one.
    // Kotlin preserves the ordering for us by associating into a LinkedHashMap.
    private val supportedTypes: Map<Class<Any>, Class<out Collection<*>>> = listOf(
        List::class.java,
        NonEmptySet::class.java,
        NavigableSet::class.java,
        SortedSet::class.java,
        Set::class.java,
        Collection::class.java
    ).associateBy {
        classLoader.loadClassForSandbox(it)
    }

    private fun getBestMatchFor(type: Class<Any>): Map.Entry<Class<Any>, Class<out Collection<*>>>
        = supportedTypes.entries.first { it.key.isAssignableFrom(type) }

    override val schemaForDocumentation: Schema = Schema(emptyList())

    override fun isSerializerFor(clazz: Class<*>): Boolean {
        return super.isSerializerFor(clazz) && unsupportedTypes.none { it.isAssignableFrom(clazz) }
    }

    override fun specialiseFor(declaredType: Type): AMQPSerializer<Any>? {
        if (declaredType !is ParameterizedType) {
            return null
        }

        @Suppress("unchecked_cast")
        val rawType = declaredType.rawType as Class<Any>
        return ConcreteCollectionSerializer(declaredType, getBestMatchFor(rawType), creator, localFactory)
    }

    override fun readObject(obj: Any, schemas: SerializationSchemas, input: DeserializationInput, context: SerializationContext): Any {
        throw UnsupportedOperationException("Factory only")
    }

    override fun writeDescribedObject(obj: Any, data: Data, type: Type, output: SerializationOutput, context: SerializationContext) {
        throw UnsupportedOperationException("Factory Only")
    }
}

private class ConcreteCollectionSerializer(
    declaredType: ParameterizedType,
    private val matchingType: Map.Entry<Class<Any>, Class<out Collection<*>>>,
    private val creator: Function<Array<Any>, out Any?>,
    factory: LocalSerializerFactory
) : AMQPSerializer<Any> {
    override val type: ParameterizedType = declaredType

    override val typeDescriptor: Symbol by lazy {
        factory.createDescriptor(
            LocalTypeInformation.ACollection(
                observedType = declaredType.rawType,
                typeIdentifier = TypeIdentifier.forGenericType(declaredType),
                elementType =factory.getTypeInformation(declaredType.actualTypeArguments[0])
            )
        )
    }

    override fun readObject(
        obj: Any,
        schemas: SerializationSchemas,
        input: DeserializationInput,
        context: SerializationContext
    ): Any {
        val inboundType = type.actualTypeArguments[0]
        return ifThrowsAppend({ type.typeName }) {
            val args = (obj as List<*>).map {
                input.readObjectOrNull(redescribe(it, inboundType), schemas, inboundType, context)
            }.toTypedArray()
            creator.apply(arrayOf(matchingType.key, args))!!
        }
    }

    override fun writeClassInfo(output: SerializationOutput) {
        abortReadOnly()
    }

    override fun writeObject(obj: Any, data: Data, type: Type, output: SerializationOutput, context: SerializationContext, debugIndent: Int) {
        abortReadOnly()
    }
}
