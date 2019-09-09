package net.corda.djvm.serialization.serializers

import net.corda.core.serialization.SerializationContext
import net.corda.djvm.rewiring.SandboxClassLoader
import net.corda.djvm.serialization.deserializers.X509CRLDeserializer
import net.corda.djvm.serialization.toSandboxAnyClass
import net.corda.serialization.internal.amqp.*
import org.apache.qpid.proton.codec.Data
import java.lang.reflect.Type
import java.security.cert.X509CRL
import java.util.Collections.singleton
import java.util.function.Function

class SandboxX509CRLSerializer(
    classLoader: SandboxClassLoader,
    taskFactory: Function<in Any, out Function<in Any?, out Any?>>
) : CustomSerializer.Implements<Any>(classLoader.toSandboxAnyClass(X509CRL::class.java)) {
    @Suppress("unchecked_cast")
    private val generator: Function<ByteArray, out Any?>
        = classLoader.createTaskFor(taskFactory, X509CRLDeserializer::class.java) as Function<ByteArray, out Any?>

    override val schemaForDocumentation: Schema = Schema(emptyList())

    override val deserializationAliases: Set<Class<*>> = singleton(X509CRL::class.java)

    override fun readObject(obj: Any, schemas: SerializationSchemas, input: DeserializationInput, context: SerializationContext): Any {
        val bits = input.readObject(obj, schemas, ByteArray::class.java, context) as ByteArray
        return generator.apply(bits)!!
    }

    override fun writeDescribedObject(obj: Any, data: Data, type: Type, output: SerializationOutput, context: SerializationContext) {
        abortReadOnly()
    }
}
