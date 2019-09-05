package net.corda.djvm.serialization.serializers

import net.corda.core.serialization.SerializationContext
import net.corda.djvm.rewiring.SandboxClassLoader
import net.corda.djvm.serialization.deserializers.UnsignedIntegerDeserializer
import net.corda.djvm.serialization.toSandboxAnyClass
import net.corda.serialization.internal.amqp.*
import org.apache.qpid.proton.amqp.UnsignedInteger
import org.apache.qpid.proton.codec.Data
import java.lang.reflect.Type
import java.util.function.Function

class SandboxUnsignedIntegerSerializer(
    classLoader: SandboxClassLoader,
    executor: Function<in Any, out Function<in Any?, out Any?>>
) : CustomSerializer.Is<Any>(classLoader.toSandboxAnyClass(UnsignedInteger::class.java)) {
    @Suppress("unchecked_cast")
    private val transformer: Function<IntArray, out Any?>
        = classLoader.createTaskFor(executor, UnsignedIntegerDeserializer::class.java) as Function<IntArray, out Any?>

    override val schemaForDocumentation: Schema = Schema(emptyList())

    override fun readObject(obj: Any, schemas: SerializationSchemas, input: DeserializationInput, context: SerializationContext): Any {
        return transformer.apply(intArrayOf((obj as UnsignedInteger).toInt()))!!
    }

    override fun writeDescribedObject(obj: Any, data: Data, type: Type, output: SerializationOutput, context: SerializationContext) {
        abortReadOnly()
    }
}
