package net.corda.djvm.serialization

import net.corda.core.serialization.CordaSerializable
import net.corda.core.serialization.SerializedBytes
import net.corda.core.serialization.deserialize
import net.corda.core.serialization.internal._contextSerializationEnv
import net.corda.core.serialization.serialize
import net.corda.djvm.serialization.SandboxType.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.fail
import java.util.function.Function

@ExtendWith(LocalSerialization::class)
class DeserializeStringTest : TestBase(KOTLIN) {
    @Test
    fun `test deserializing string`() {
        val stringMessage = StringMessage("Hello World!")
        val data = SerializedBytes<Any>(stringMessage.serialize().bytes)

        sandbox {
            _contextSerializationEnv.set(createSandboxSerializationEnv(classLoader))

            val sandboxString = data.deserialize()

            val executor = createExecutorFor(classLoader)
            val result = executor.apply(
                classLoader.loadClassForSandbox(ShowStringMessage::class.java).newInstance(),
                sandboxString
            ) ?: fail("Result cannot be null")

            assertEquals(result.toString(), stringMessage.message)
        }
    }

    class ShowStringMessage : Function<StringMessage, String> {
        override fun apply(data: StringMessage): String {
            return data.message
        }
    }

    @Test
    fun `test deserializing string array`() {
        val stringArray = StringArray(arrayOf("Hello", "World", "!"))
        val data = SerializedBytes<Any>(stringArray.serialize().bytes)

        sandbox {
            _contextSerializationEnv.set(createSandboxSerializationEnv(classLoader))

            val sandboxArray = data.deserialize()

            val executor = createExecutorFor(classLoader)
            val result = executor.apply(
                classLoader.loadClassForSandbox(ShowStringArray::class.java).newInstance(),
                sandboxArray
            ) ?: fail("Result cannot be null")

            assertEquals(result.toString(), stringArray.lines.joinToString())
        }
    }

    class ShowStringArray : Function<StringArray, String> {
        override fun apply(data: StringArray): String {
            return data.lines.joinToString()
        }
    }

    @Test
    fun `test deserializing string list`() {
        val stringList = StringList(listOf("Hello", "World", "!"))
        val data = SerializedBytes<Any>(stringList.serialize().bytes)

        sandbox {
            _contextSerializationEnv.set(createSandboxSerializationEnv(classLoader))

            val sandboxList = data.deserialize()

            val executor = createExecutorFor(classLoader)
            val result = executor.apply(
                classLoader.loadClassForSandbox(ShowStringList::class.java).newInstance(),
                sandboxList
            ) ?: fail("Result cannot be null")

            assertEquals(result.toString(), stringList.lines.joinToString())
        }
    }

    class ShowStringList : Function<StringList, String> {
        override fun apply(data: StringList): String {
            return data.lines.joinToString()
        }
    }

    @Test
    fun `test deserializing string list of arrays`() {
        val stringListArray = StringListOfArray(listOf(
            arrayOf("Hello"), arrayOf("World"), arrayOf("!"))
        )
        val data = SerializedBytes<Any>(stringListArray.serialize().bytes)

        sandbox {
            _contextSerializationEnv.set(createSandboxSerializationEnv(classLoader))

            val sandboxListArray = data.deserialize()

            val executor = createExecutorFor(classLoader)
            val result = executor.apply(
                classLoader.loadClassForSandbox(ShowStringListOfArray::class.java).newInstance(),
                sandboxListArray
            ) ?: fail("Result cannot be null")

            assertEquals(result.toString(), stringListArray.data.flatMap(Array<String>::toList).joinToString())
        }
    }

    class ShowStringListOfArray : Function<StringListOfArray, String> {
        override fun apply(obj: StringListOfArray): String {
            return obj.data.flatMap(Array<String>::toList).joinToString()
        }
    }
}

@CordaSerializable
data class StringMessage(val message: String)

@CordaSerializable
class StringArray(val lines: Array<String>)

@CordaSerializable
class StringList(val lines: List<String>)

@CordaSerializable
class StringListOfArray(val data: List<Array<String>>)