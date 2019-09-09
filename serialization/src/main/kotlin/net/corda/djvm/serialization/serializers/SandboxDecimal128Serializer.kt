package net.corda.djvm.serialization.serializers

import net.corda.core.serialization.SerializationContext
import net.corda.djvm.rewiring.SandboxClassLoader
import net.corda.djvm.serialization.deserializers.Decimal128Deserializer
import net.corda.djvm.serialization.toSandboxAnyClass
import net.corda.serialization.internal.amqp.*
import org.apache.qpid.proton.amqp.Decimal128
import org.apache.qpid.proton.codec.Data
import java.lang.reflect.Type
import java.util.function.Function

class SandboxDecimal128Serializer(
    classLoader: SandboxClassLoader,
    taskFactory: Function<in Any, out Function<in Any?, out Any?>>
) : CustomSerializer.Is<Any>(classLoader.toSandboxAnyClass(Decimal128::class.java)) {
    @Suppress("unchecked_cast")
    private val transformer: Function<LongArray, out Any?>
            = classLoader.createTaskFor(taskFactory, Decimal128Deserializer::class.java) as Function<LongArray, out Any?>

    override val schemaForDocumentation: Schema = Schema(emptyList())

    override fun readObject(obj: Any, schemas: SerializationSchemas, input: DeserializationInput, context: SerializationContext): Any {
        val decimal128 = obj as Decimal128
        return transformer.apply(longArrayOf(decimal128.mostSignificantBits, decimal128.leastSignificantBits))!!
    }

    override fun writeDescribedObject(obj: Any, data: Data, type: Type, output: SerializationOutput, context: SerializationContext) {
        abortReadOnly()
    }
}
