package net.corda.djvm.execution

import net.corda.djvm.SandboxType.KOTLIN
import net.corda.djvm.TestBase
import net.corda.djvm.rewiring.ByteCode
import net.corda.djvm.rewiring.ByteCodeKey
import net.corda.djvm.rewiring.SandboxClassLoader
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import sandbox.SandboxFunction
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Function

class ExternalByteCodeCacheTest : TestBase(KOTLIN) {
    @Test
    fun testSubsequentSandboxesWithCaching() {
        // Ensure that the shared parent configuration's internal
        // cache does not already contain the Function class.
        flushParentCache()

        val externalCache = ConcurrentHashMap<ByteCodeKey, ByteCode>()
        sandbox(externalCache) {
            assertTrue((classLoader.parent as SandboxClassLoader).externalCaching)
            assertTrue(classLoader.externalCaching)
        }
        assertThat(externalCache).isEmpty()

        sandbox(externalCache) {
            assertTrue((classLoader.parent as SandboxClassLoader).externalCaching)
            assertTrue(classLoader.externalCaching)
            classLoader.loadForSandbox(ExternalTask::class.java.name)
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
            classLoader.loadForSandbox(ExternalTask::class.java.name)
        }
        assertThat(externalCache).isEmpty()
    }

    @Test
    fun testExternalCacheIsUsed() {
        val externalCache = ConcurrentHashMap<ByteCodeKey, ByteCode>()
        sandbox(externalCache) {
            (classLoader.parent as SandboxClassLoader).externalCaching = false
            classLoader.loadForSandbox(ExternalTask::class.java.name)
        }
        assertThat(externalCache).hasSize(1)

        // Replace the entry inside the external cache with nonsense.
        val key = externalCache.keys.first()
        externalCache[key] = ByteCode(byteArrayOf(), false)

        // We cannot create a class without any byte-code!
        val ex = assertThrows<ClassFormatError>() {
            sandbox(externalCache) {
                classLoader.loadForSandbox(ExternalTask::class.java.name)
            }
        }
        assertThat(ex).hasMessage("Truncated class file")
    }

    @Test
    fun testInternalCacheTakesPrecedenceOverExternalCache() {
        // Ensure that the shared parent configuration's internal
        // cache does not already contain the Function class.
        flushParentCache()

        val externalCache = ConcurrentHashMap<ByteCodeKey, ByteCode>()
        sandbox(externalCache) {
            classLoader.loadForSandbox(Function::class.java.name)
        }
        assertThat(externalCache).hasSize(1)

        // Replace the entry inside the external cache with nonsense.
        val key = externalCache.keys.first()
        externalCache[key] = ByteCode(byteArrayOf(), false)

        sandbox(externalCache) {
            classLoader.loadForSandbox(ExternalTask::class.java.name)
        }
        assertThat(externalCache).hasSize(2)
    }

    class ExternalTask : Function<String, String> {
        override fun apply(input: String): String {
            return input
        }
    }
}
