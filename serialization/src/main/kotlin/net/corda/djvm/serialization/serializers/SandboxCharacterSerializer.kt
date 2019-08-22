package net.corda.djvm.serialization.serializers

import net.corda.core.serialization.SerializationContext
import net.corda.djvm.rewiring.SandboxClassLoader
import net.corda.djvm.serialization.loadClassForSandbox
import net.corda.serialization.internal.amqp.*
import org.apache.qpid.proton.codec.Data
import java.lang.reflect.Type
import java.util.function.UnaryOperator

class SandboxCharacterSerializer(
    classLoader: SandboxClassLoader,
    private val basicInput: UnaryOperator<in Any?>
) : CustomSerializer.Is<Any>(classLoader.loadClassForSandbox(Char::class.javaObjectType)) {

    override val schemaForDocumentation: Schema = Schema(emptyList())

    override fun readObject(obj: Any, schemas: SerializationSchemas, input: DeserializationInput, context: SerializationContext): Any {
        return basicInput.apply((obj as? Short)?.toChar() ?: obj)!!
    }

    override fun writeDescribedObject(obj: Any, data: Data, type: Type, output: SerializationOutput, context: SerializationContext) {
       abortReadOnly()
    }
}
