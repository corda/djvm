package net.corda.djvm.serialization

import net.corda.core.serialization.CordaSerializable
import net.corda.core.serialization.SerializedBytes
import net.corda.core.serialization.deserialize
import net.corda.core.serialization.internal._contextSerializationEnv
import net.corda.core.serialization.serialize
import net.corda.core.utilities.NonEmptySet
import net.corda.djvm.serialization.SandboxType.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.fail
import java.util.*
import java.util.function.Function

@ExtendWith(LocalSerialization::class)
class DeserializeCollectionsTest : TestBase(KOTLIN) {
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

            assertEquals(stringList.lines.joinToString(), result.toString())
            assertEquals("Hello, World, !", result.toString())
        }
    }

    class ShowStringList : Function<StringList, String> {
        override fun apply(data: StringList): String {
            return data.lines.joinToString()
        }
    }

    @Test
    fun `test deserializing integer set`() {
        val integerSet = IntegerSet(linkedSetOf(10, 3, 15, 2, 10))
        val data = SerializedBytes<Any>(integerSet.serialize().bytes)

        sandbox {
            _contextSerializationEnv.set(createSandboxSerializationEnv(classLoader))

            val sandboxSet = data.deserialize()

            val executor = createExecutorFor(classLoader)
            val result = executor.apply(
                classLoader.loadClassForSandbox(ShowIntegerSet::class.java).newInstance(),
                sandboxSet
            ) ?: fail("Result cannot be null")

            assertEquals(integerSet.numbers.joinToString(), result.toString())
            assertEquals("10, 3, 15, 2", result.toString())
        }
    }

    class ShowIntegerSet : Function<IntegerSet, String> {
        override fun apply(data: IntegerSet): String {
            return data.numbers.joinToString()
        }
    }

    @Test
    fun `test deserializing integer sorted set`() {
        val integerSortedSet = IntegerSortedSet(sortedSetOf(10, 15, 1000, 3, 2, 10))
        val data = SerializedBytes<Any>(integerSortedSet.serialize().bytes)

        sandbox {
            _contextSerializationEnv.set(createSandboxSerializationEnv(classLoader))

            val sandboxSet = data.deserialize()

            val executor = createExecutorFor(classLoader)
            val result = executor.apply(
                classLoader.loadClassForSandbox(ShowIntegerSortedSet::class.java).newInstance(),
                sandboxSet
            ) ?: fail("Result cannot be null")

            assertEquals(integerSortedSet.numbers.joinToString(), result.toString())
            assertEquals("2, 3, 10, 15, 1000", result.toString())
        }
    }

    class ShowIntegerSortedSet : Function<IntegerSortedSet, String> {
        override fun apply(data: IntegerSortedSet): String {
            return data.numbers.joinToString()
        }
    }

    @Test
    fun `test deserializing long navigable set`() {
        val longNavigableSet = LongNavigableSet(sortedSetOf(99955L, 10, 15, 1000, 3, 2, 10))
        val data = SerializedBytes<Any>(longNavigableSet.serialize().bytes)

        sandbox {
            _contextSerializationEnv.set(createSandboxSerializationEnv(classLoader))

            val sandboxSet = data.deserialize()

            val executor = createExecutorFor(classLoader)
            val result = executor.apply(
                classLoader.loadClassForSandbox(ShowLongNavigableSet::class.java).newInstance(),
                sandboxSet
            ) ?: fail("Result cannot be null")

            assertEquals(longNavigableSet.numbers.joinToString(), result.toString())
            assertEquals("2, 3, 10, 15, 1000, 99955", result.toString())
        }
    }

    class ShowLongNavigableSet : Function<LongNavigableSet, String> {
        override fun apply(data: LongNavigableSet): String {
            return data.numbers.joinToString()
        }
    }

    @Test
    fun `test deserializing short collection`() {
        val shortCollection = ShortCollection(listOf(10, 200, 3000))
        val data = SerializedBytes<Any>(shortCollection.serialize().bytes)

        sandbox {
            _contextSerializationEnv.set(createSandboxSerializationEnv(classLoader))

            val sandboxCollection = data.deserialize()

            val executor = createExecutorFor(classLoader)
            val result = executor.apply(
                classLoader.loadClassForSandbox(ShowShortCollection::class.java).newInstance(),
                sandboxCollection
            ) ?: fail("Result cannot be null")

            assertEquals(shortCollection.numbers.joinToString(), result.toString())
            assertEquals("10, 200, 3000", result.toString())
        }
    }

    class ShowShortCollection : Function<ShortCollection, String> {
        override fun apply(data: ShortCollection): String {
            return data.numbers.joinToString()
        }
    }

    @Test
    fun `test deserializing non-empty string set`() {
        val nonEmptyStrings = NonEmptyStringSet(NonEmptySet.of("Hello", "World", "!"))
        val data = SerializedBytes<Any>(nonEmptyStrings.serialize().bytes)

        sandbox {
            _contextSerializationEnv.set(createSandboxSerializationEnv(classLoader))

            val sandboxSet = data.deserialize()

            val executor = createExecutorFor(classLoader)
            val result = executor.apply(
                classLoader.loadClassForSandbox(ShowNonEmptyStringSet::class.java).newInstance(),
                sandboxSet
            ) ?: fail("Result cannot be null")

            assertEquals(nonEmptyStrings.lines.joinToString(), result.toString())
            assertEquals("Hello, World, !", result.toString())
        }
    }

    class ShowNonEmptyStringSet : Function<NonEmptyStringSet, String> {
        override fun apply(data: NonEmptyStringSet): String {
            return data.lines.joinToString()
        }
    }
}

@CordaSerializable
class StringList(val lines: List<String>)

@CordaSerializable
class IntegerSet(val numbers: Set<Int>)

@CordaSerializable
class IntegerSortedSet(val numbers: SortedSet<Int>)

@CordaSerializable
class LongNavigableSet(val numbers: NavigableSet<Long>)

@CordaSerializable
class ShortCollection(val numbers: Collection<Short>)

@CordaSerializable
class NonEmptyStringSet(val lines: NonEmptySet<String>)
