package net.corda.djvm.serialization.serializers

import net.corda.core.serialization.SerializationContext
import net.corda.djvm.rewiring.SandboxClassLoader
import net.corda.djvm.serialization.toSandboxAnyClass
import net.corda.serialization.internal.amqp.*
import org.apache.qpid.proton.codec.Data
import java.lang.reflect.Type
import java.util.function.Function

class SandboxCharacterSerializer(
    classLoader: SandboxClassLoader,
    private val basicInput: Function<in Any?, out Any?>
) : CustomSerializer.Is<Any>(classLoader.toSandboxAnyClass(Char::class.javaObjectType)) {

    override val schemaForDocumentation: Schema = Schema(emptyList())

    override fun readObject(obj: Any, schemas: SerializationSchemas, input: DeserializationInput, context: SerializationContext): Any {
        return basicInput.apply(convertToChar(obj))!!
    }

    private fun convertToChar(obj: Any): Any {
        return when (obj) {
            is Short -> obj.toChar()
            is Int -> obj.toChar()
            else -> obj
        }
    }

    override fun writeDescribedObject(obj: Any, data: Data, type: Type, output: SerializationOutput, context: SerializationContext) {
       abortReadOnly()
    }
}
