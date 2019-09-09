package net.corda.djvm.serialization.serializers

import net.corda.core.serialization.SerializationContext
import net.corda.djvm.rewiring.SandboxClassLoader
import net.corda.djvm.serialization.deserializers.CreateFromString
import net.corda.djvm.serialization.toSandboxAnyClass
import net.corda.serialization.internal.amqp.*
import org.apache.qpid.proton.codec.Data
import java.lang.reflect.Constructor
import java.lang.reflect.Type
import java.util.Collections.singleton
import java.util.function.Function

class SandboxToStringSerializer(
    unsafeClass: Class<*>,
    classLoader: SandboxClassLoader,
    taskFactory: Function<in Any, out Function<in Any?, out Any?>>,
    basicInput: Function<in Any?, out Any?>
) : CustomSerializer.Is<Any>(classLoader.toSandboxAnyClass(unsafeClass)) {
    private val creator: Function<Any?, Any?>

    init {
        val stringClass = classLoader.loadClass("sandbox.java.lang.String")
        val createTask = classLoader.toSandboxClass(CreateFromString::class.java)
            .getConstructor(Constructor::class.java)
            .newInstance(clazz.getConstructor(stringClass))
        creator = basicInput.andThen(taskFactory.apply(createTask))
    }

    override val deserializationAliases: Set<Class<*>> = singleton(unsafeClass)

    override val schemaForDocumentation: Schema = Schema(emptyList())

    override fun readObject(obj: Any, schemas: SerializationSchemas, input: DeserializationInput, context: SerializationContext): Any {
        return creator.apply(obj)!!
    }

    override fun writeDescribedObject(obj: Any, data: Data, type: Type, output: SerializationOutput, context: SerializationContext) {
        abortReadOnly()
    }
}
