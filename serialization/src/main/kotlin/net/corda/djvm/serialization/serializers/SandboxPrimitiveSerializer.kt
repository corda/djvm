package net.corda.djvm.serialization.serializers

import net.corda.core.serialization.SerializationContext
import net.corda.djvm.rewiring.SandboxClassLoader
import net.corda.djvm.serialization.loadClassForSandbox
import net.corda.serialization.internal.amqp.*
import org.apache.qpid.proton.codec.Data
import java.lang.reflect.Type
import java.util.function.UnaryOperator

class SandboxPrimitiveSerializer(
    classLoader: SandboxClassLoader,
    private val basicInput: UnaryOperator<Any?>
) : CustomSerializer.Is<Any>(classLoader.loadClassForSandbox(String::class.java)) {
    override val deserializationAliases = setOf(
        Char::class.javaObjectType,
        Byte::class.javaObjectType,
        Short::class.javaObjectType,
        Int::class.javaObjectType,
        Long::class.javaObjectType,
        Float::class.javaObjectType,
        Double::class.javaObjectType,
        Boolean::class.javaObjectType
    ).map { classLoader.loadClassForSandbox(it) }.toSet()

    override val schemaForDocumentation: Schema = Schema(emptyList())

    override fun readObject(obj: Any, schemas: SerializationSchemas, input: DeserializationInput, context: SerializationContext): Any {
        return basicInput.apply(obj)!!
    }

    override fun writeDescribedObject(obj: Any, data: Data, type: Type, output: SerializationOutput, context: SerializationContext) {
        throw UnsupportedOperationException("Read Only!")
    }
}
