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
import java.util.*
import java.util.function.Function

@ExtendWith(LocalSerialization::class)
class DeserializeMapsTest : TestBase(KOTLIN) {
    @Test
    fun `test deserializing map`() {
        val stringMap = StringMap(mapOf("Open" to "Hello World", "Close" to "Goodbye, Cruel World"))
        val data = SerializedBytes<Any>(stringMap.serialize().bytes)

        sandbox {
            _contextSerializationEnv.set(createSandboxSerializationEnv(classLoader))

            val sandboxMap = data.deserialize()

            val executor = createExecutorFor(classLoader)
            val result = executor.apply(
                classLoader.loadClassForSandbox(ShowStringMap::class.java).newInstance(),
                sandboxMap
            ) ?: fail("Result cannot be null")

            assertEquals(stringMap.values.entries.joinToString(), result.toString())
            assertEquals("Open=Hello World, Close=Goodbye, Cruel World", result.toString())
        }
    }

    class ShowStringMap : Function<StringMap, String> {
        override fun apply(data: StringMap): String {
            return data.values.entries.joinToString()
        }
    }

    @Test
    fun `test deserializing sorted map`() {
        val sortedMap = StringSortedMap(sortedMapOf(
            100 to "Goodbye, Cruel World",
            10 to "Hello World",
            50 to "Having Fun!"
        ))
        val data = SerializedBytes<Any>(sortedMap.serialize().bytes)

        sandbox {
            _contextSerializationEnv.set(createSandboxSerializationEnv(classLoader))

            val sandboxMap = data.deserialize()

            val executor = createExecutorFor(classLoader)
            val result = executor.apply(
                classLoader.loadClassForSandbox(ShowStringSortedMap::class.java).newInstance(),
                sandboxMap
            ) ?: fail("Result cannot be null")

            assertEquals(sortedMap.values.entries.joinToString(), result.toString())
            assertEquals("10=Hello World, 50=Having Fun!, 100=Goodbye, Cruel World", result.toString())
        }
    }

    class ShowStringSortedMap : Function<StringSortedMap, String> {
        override fun apply(data: StringSortedMap): String {
            return data.values.entries.joinToString()
        }
    }

    @Test
    fun `test deserializing navigable map`() {
        val navigableMap = StringNavigableMap(mapOf(
            10000L to "Goodbye, Cruel World",
            1000L to "Hello World",
            5000L to "Having Fun!"
        ).toMap(TreeMap()))
        val data = SerializedBytes<Any>(navigableMap.serialize().bytes)

        sandbox {
            _contextSerializationEnv.set(createSandboxSerializationEnv(classLoader))

            val sandboxMap = data.deserialize()

            val executor = createExecutorFor(classLoader)
            val result = executor.apply(
                classLoader.loadClassForSandbox(ShowStringNavigableMap::class.java).newInstance(),
                sandboxMap
            ) ?: fail("Result cannot be null")

            assertEquals(navigableMap.values.entries.joinToString(), result.toString())
            assertEquals("1000=Hello World, 5000=Having Fun!, 10000=Goodbye, Cruel World", result.toString())
        }
    }

    class ShowStringNavigableMap : Function<StringNavigableMap, String> {
        override fun apply(data: StringNavigableMap): String {
            return data.values.entries.joinToString()
        }
    }
}

@CordaSerializable
class StringMap(val values: Map<String, String>)

@CordaSerializable
class StringSortedMap(val values: SortedMap<Int, String>)

@CordaSerializable
class StringNavigableMap(val values: NavigableMap<Long, String>)
