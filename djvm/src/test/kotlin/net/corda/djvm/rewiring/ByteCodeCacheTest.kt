package net.corda.djvm.rewiring

import net.corda.djvm.analysis.AnalysisConfiguration
import net.corda.djvm.analysis.Whitelist
import net.corda.djvm.source.UserPathSource
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ByteCodeCacheTest {
    @Test
    fun testCacheWithNoParent() {
        val configuration = createAnalysisConfiguration()
        assertNull(configuration.parent)

        val byteCodeCache = ByteCodeCache.createFor(configuration)
        assertNull(byteCodeCache.parent)
    }

    @Test
    fun testCacheWithParent() {
        val parentConfiguration = createAnalysisConfiguration()
        val configuration = parentConfiguration.createChild(UserPathSource(emptyList())).build()
        assertNotNull(configuration.parent)
        assertNull(configuration.parent!!.parent)

        val byteCodeCache = ByteCodeCache.createFor(configuration)
        assertNotNull(byteCodeCache.parent)
        assertNull(byteCodeCache.parent!!.parent)
    }

    private fun createAnalysisConfiguration() = AnalysisConfiguration.createRoot(
        userSource = UserPathSource(emptyList()),
        whitelist = Whitelist.EMPTY,
        visibleAnnotations = emptySet()
    )
}
