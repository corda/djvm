package net.corda.djvm

import net.corda.djvm.SandboxType.KOTLIN
import net.corda.djvm.rewiring.ByteCode
import net.corda.djvm.rewiring.ByteCodeKey
import net.corda.djvm.rewiring.SandboxClassLoader
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.concurrent.ConcurrentHashMap

class SyntheticExceptionCachingTest : TestBase(KOTLIN) {
    @ParameterizedTest(name = "[{index}] = {0}")
    @ValueSource(strings = [
        "sandbox.java.security.NoSuchAlgorithmException",
        "sandbox.net.corda.djvm.execution.MyExampleException"
    ])
    fun testSyntheticExceptionIsRecreatedWithExternalCache(exceptionName: String) {
        val externalCache = ConcurrentHashMap<ByteCodeKey, ByteCode>()
        val syntheticClassName = "$exceptionName\$1DJVM"

        sandbox(externalCache) {
            assertThat(classLoader.parent).isInstanceOf(SandboxClassLoader::class.java)
            classLoader.loadClass(syntheticClassName).asSubclass(Throwable::class.java)
        }

        // Check that the byte-code has also been cached correctly.
        // The synthetic exception class is cheap to create and so is not cached.
        val classNames = externalCache.keys.mapTo(LinkedHashSet(), ByteCodeKey::className)
        assertThat(classNames as Iterable<String>)
            .doesNotContain(syntheticClassName)
            .contains(exceptionName)

        sandbox(externalCache) {
            assertThat(classLoader.parent).isInstanceOf(SandboxClassLoader::class.java)

            // Empty this configuration's internal cache, which
            // will force us to consult the external cache.
            flushInternalCache()

            assertDoesNotThrow { classLoader.loadClass(syntheticClassName).asSubclass(Throwable::class.java) }
            assertDoesNotThrow { classLoader.loadClass(exceptionName) }

            // Check we can reload these classes, to prove they were loaded correctly!
            assertDoesNotThrow { classLoader.loadClass(syntheticClassName).asSubclass(Throwable::class.java) }
            assertDoesNotThrow { classLoader.loadClass(exceptionName) }
        }
    }
}