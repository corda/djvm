package net.corda.djvm.execution

import net.corda.djvm.SandboxType.KOTLIN
import net.corda.djvm.TestBase
import net.corda.djvm.rewiring.ByteCode
import net.corda.djvm.rewiring.ByteCodeKey
import net.corda.djvm.rewiring.SandboxClassLoader
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import sandbox.SandboxFunction
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Function

class ExternalByteCodeCacheTest : TestBase(KOTLIN) {
    @Test
    fun testSubsequentSandboxesWithCaching() {
        // Ensure that the shared parent configuration's internal
        // cache does not already contain the Function class.
        flushInternalCache()

        val externalCache = ConcurrentHashMap<ByteCodeKey, ByteCode>()
        sandbox(externalCache) {
            assertTrue((classLoader.parent as SandboxClassLoader).externalCaching)
            assertTrue(classLoader.externalCaching)
        }
        assertThat(externalCache).isEmpty()

        sandbox(externalCache) {
            assertTrue((classLoader.parent as SandboxClassLoader).externalCaching)
            assertTrue(classLoader.externalCaching)
            classLoader.toSandboxClass(ExternalTask::class.java.name)
        }

        val keyNames = externalCache.keys.mapTo(ArrayList(), ByteCodeKey::className)
        assertThat(keyNames).containsExactlyInAnyOrder(
            "sandbox." + ExternalTask::class.java.name,
            SandboxFunction::class.java.name
        )
    }

    @Test
    fun testDisablingExternalCache() {
        val externalCache = ConcurrentHashMap<ByteCodeKey, ByteCode>()
        sandbox(externalCache) {
            assertTrue((classLoader.parent as SandboxClassLoader).externalCaching)
            assertTrue(classLoader.externalCaching)
        }
        assertThat(externalCache).isEmpty()

        sandbox(externalCache) {
            (classLoader.parent as SandboxClassLoader).externalCaching = false
            classLoader.externalCaching = false
            classLoader.toSandboxClass(ExternalTask::class.java)
        }
        assertThat(externalCache).isEmpty()
    }

    @Test
    fun testExternalCacheIsUsed() {
        val externalCache = ConcurrentHashMap<ByteCodeKey, ByteCode>()
        sandbox(externalCache) {
            (classLoader.parent as SandboxClassLoader).externalCaching = false
            classLoader.toSandboxClass(ExternalTask::class.java)
        }
        assertThat(externalCache).hasSize(1)

        // Replace the entry inside the external cache with nonsense.
        val key = externalCache.keys.first()
        externalCache[key] = ByteCode(byteArrayOf(), null)

        // We cannot create a class without any byte-code!
        val ex = assertThrows<ClassFormatError> {
            sandbox(externalCache) {
                classLoader.toSandboxClass(ExternalTask::class.java)
            }
        }
        assertThat(ex).hasMessage("Truncated class file")
    }

    @Test
    fun testInternalCacheTakesPrecedenceOverExternalCache() {
        // Ensure that the shared parent configuration's internal
        // cache does not already contain the Function class.
        flushInternalCache()

        val externalCache = ConcurrentHashMap<ByteCodeKey, ByteCode>()
        sandbox(externalCache) {
            classLoader.toSandboxClass(Function::class.java)
        }
        assertThat(externalCache).hasSize(1)

        // Replace the entry inside the external cache with nonsense.
        val key = externalCache.keys.first()
        externalCache[key] = ByteCode(byteArrayOf(), null)

        sandbox(externalCache) {
            classLoader.toSandboxClass(ExternalTask::class.java)
        }
        assertThat(externalCache).hasSize(2)
    }

    class ExternalTask : Function<String, String> {
        override fun apply(input: String): String {
            return input
        }
    }

    @Test
    fun testExternalCacheKeysHaveValidLocations() {
        // Empty the shared parent configuration's internal cache.
        flushInternalCache()

        val externalCache = ConcurrentHashMap<ByteCodeKey, ByteCode>()
        sandbox(externalCache) {
            classLoader.toSandboxClass(ExternalTask::class.java)
            classLoader.toSandboxClass(ExampleEnum::class.java)
            classLoader.toSandboxClass(MyExampleException::class.java)
        }

        val cacheKeys = externalCache.keys
        assertAll(cacheKeys.map { key -> {
            val url = assertDoesNotThrow { URL(key.source) }
            assertEquals("file", url.protocol)
            assertTrue(url.path.endsWith('/') || url.path.endsWith(".jar"),
                "Invalid path: '${url.path}'")
        }})
    }

    @Test
    fun testExternalCacheIsCorrectlyFormed() {
        val externalCache = ConcurrentHashMap<ByteCodeKey, ByteCode>()

        flushInternalCache()
        sandbox(externalCache) {
            classLoader.toSandboxClass(Function::class.java)
        }

        flushInternalCache()
        sandbox(externalCache) {
            classLoader.toSandboxClass(ExternalTask::class.java)
        }

        flushInternalCache()
        sandbox(externalCache) {
            classLoader.toSandboxClass(ExampleEnum::class.java)
        }

        flushInternalCache()
        sandbox(externalCache) {
            classLoader.toSandboxClass(MyExampleException::class.java)
        }

        assertThat(externalCache)
            .hasSizeGreaterThanOrEqualTo(4)

        val sourceLocations = mutableMapOf<String, String>()
        assertAll(externalCache.map { entry -> {
            val location = entry.key.source.let { source ->
                sourceLocations.putIfAbsent(source, source) ?: source
            }
            assertSame(entry.key.source, location)
        }})
    }
}
