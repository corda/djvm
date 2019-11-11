package net.corda.djvm.execution

import net.corda.djvm.SandboxType.KOTLIN
import net.corda.djvm.TestBase
import net.corda.djvm.rewiring.ByteCode
import net.corda.djvm.rewiring.ByteCodeKey
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import sandbox.SandboxFunction
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Function
import java.util.function.Supplier

class ExternalByteCodeCacheTest : TestBase(KOTLIN) {
    @Test
    fun testSubsequentSandboxesWithCaching() {
        val externalCache = ConcurrentHashMap<ByteCodeKey, ByteCode>()
        sandbox(externalCache) {
            assertTrue(classLoader.externalCaching)
        }
        assertThat(externalCache).isEmpty()

        sandbox(externalCache) {
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
            assertTrue(classLoader.externalCaching)
        }
        assertThat(externalCache).isEmpty()

        sandbox(externalCache) {
            classLoader.externalCaching = false
            classLoader.loadForSandbox(ExternalTask::class.java.name)
        }
        assertThat(externalCache).isEmpty()
    }

    @Test
    fun testExternalCacheIsUsed() {
        val externalCache = ConcurrentHashMap<ByteCodeKey, ByteCode>()
        sandbox(externalCache) {
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

    class ExternalTask : Function<String, String> {
        override fun apply(input: String): String {
            return input
        }
    }

    /**
     * Use [Supplier] instead of [Function] to prevent this
     * test's classes overlapping with the other tests'.
     */
    @Test
    fun testInternalCacheTakesPrecedenceOverExternalCache() {
        val externalCache = ConcurrentHashMap<ByteCodeKey, ByteCode>()
        sandbox(externalCache) {
            classLoader.loadForSandbox(Supplier::class.java.name)
        }
        assertThat(externalCache).hasSize(1)

        // Replace the entry inside the external cache with nonsense.
        val key = externalCache.keys.first()
        externalCache[key] = ByteCode(byteArrayOf(), false)

        sandbox(externalCache) {
            classLoader.loadForSandbox(ExternalSupplier::class.java.name)
        }
        assertThat(externalCache).hasSize(2)
    }

    class ExternalSupplier : Supplier<String> {
        override fun get(): String {
            return "Hello World!"
        }
    }
}
