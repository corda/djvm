package net.corda.djvm

import net.corda.djvm.DummyJar.Companion.putCompressedClass
import net.corda.djvm.DummyJar.Companion.putDirectoryOf
import net.corda.djvm.DummyJar.Companion.putUncompressedEntry
import net.corda.djvm.SandboxType.KOTLIN
import net.corda.djvm.source.UserPathSource
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.function.Function
import java.util.zip.Deflater.NO_COMPRESSION

class SandboxConfigurationTest : TestBase(KOTLIN) {
    companion object {
        private lateinit var testJar: DummyJar

        @JvmStatic
        @BeforeAll
        fun setup(@TempDir testProjectDir: Path) {
            testJar = DummyJar(testProjectDir, "sandbox-configuration").build(JarWriter { jar, _ ->
                jar.setLevel(NO_COMPRESSION)
                jar.setComment("Test jar tagged for preloading")

                // Add the tag for the DJVM to find.
                jar.putUncompressedEntry("META-INF/DJVM-preload", byteArrayOf())

                // Add the class to be preloaded.
                jar.putDirectoryOf(PreloadExample::class.java)
                jar.putCompressedClass(PreloadExample::class.java)
            })
        }
    }

    @Test
    fun testPreloadingConfiguration() = sandbox {
        UserPathSource(arrayOf(testJar.path.toUri().toURL())).use { source ->
            val childConfiguration = configuration.createChild(source)

            val loadedClassNames = configuration.byteCodeCache.classNames
            val loadedParentClassNames = configuration.byteCodeCache.parent.classNames
            val loadedChildClassNames = childConfiguration.byteCodeCache.classNames

            assertThat(loadedClassNames).isEmpty()
            assertThat(loadedParentClassNames).isEmpty()
            assertThat(loadedChildClassNames).isEmpty()

            childConfiguration.preload()

            /**
             * Our [PreloadExample] class exists inside both the
             * configuration and its child configuration, each of
             * which has its own classloader. And we expect the
             * child classloader to delegate loading this class
             * to its parent.
             */
            assertThat(loadedChildClassNames).isEmpty()
            assertThat(loadedClassNames)
                .contains("sandbox." + PreloadExample::class.java.name)
            assertThat(loadedParentClassNames)
                .contains(
                    "sandbox.java.lang.Object",
                    "sandbox.java.lang.Comparable",
                    "sandbox.java.lang.StackTraceElement",
                    "sandbox.java.lang.String",
                    "sandbox.java.lang.StringBuilder",
                    "sandbox.java.lang.Throwable",
                    "sandbox.java.lang.RuntimeException",
                    "sandbox.java.lang.IllegalArgumentException",
                    "sandbox.java.util.function.Function"
                )
        }
    }

    class PreloadExample : Function<String, String> {
        override fun apply(input: String): String {
            throw IllegalArgumentException("Example says: '$input'")
        }
    }
}

