package net.corda.djvm.source

import net.corda.djvm.Action
import net.corda.djvm.analysis.ClassResolver
import net.corda.djvm.analysis.Whitelist
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class SourceClassLoaderTest {

    private val classResolver = ClassResolver(emptySet(), Whitelist.createWhitelist(), "")
    private val apiSource = BootstrapClassLoader(Paths.get(
        System.getProperty("deterministic-rt.path") ?: fail("deterministic-rt.path property not set")
    ))

    @Test
    fun `can load class from Java's lang package when no files are provided to the class loader`() {
        val classLoader = SourceClassLoader(classResolver, UserPathSource(emptyList()))
        val clazz = classLoader.loadClassHeader("java.lang.Boolean")
        assertThat(clazz.name).isEqualTo("java.lang.Boolean")
    }

    @Test
    fun `cannot load arbitrary class when no files are provided to the class loader`() {
        val classLoader = SourceClassLoader(classResolver, UserPathSource(emptyList()))
        assertThrows<ClassNotFoundException> {
            classLoader.loadClassHeader("net.foo.NonExistentClass")
        }
    }

    @Test
    fun `can load class when JAR file is provided to the class loader`() {
        useTemporaryFile("jar-with-single-class.jar") {
            val classLoader = SourceClassLoader(classResolver, UserPathSource(this))
            val clazz = classLoader.loadClassHeader("net.foo.Bar")
            assertThat(clazz.name).isEqualTo("net.foo.Bar")
        }
    }

    @Test
    fun `cannot load arbitrary class when JAR file is provided to the class loader`() {
        useTemporaryFile("jar-with-single-class.jar") {
            val classLoader = SourceClassLoader(classResolver, UserPathSource(this))
            assertThrows<ClassNotFoundException> {
                classLoader.loadClassHeader("net.foo.NonExistentClass")
            }
        }
    }

    @Test
    fun `can load classes when multiple JAR files are provided to the class loader`() {
        useTemporaryFile("jar-with-single-class.jar", "jar-with-two-classes.jar") {
            val classLoader = SourceClassLoader(classResolver, UserPathSource(this))
            val firstClass = classLoader.loadClassHeader("com.somewhere.Test")
            assertThat(firstClass.name).isEqualTo("com.somewhere.Test")
            val secondClass = classLoader.loadClassHeader("com.somewhere.AnotherTest")
            assertThat(secondClass.name).isEqualTo("com.somewhere.AnotherTest")
        }
    }

    @Test
    fun `cannot load arbitrary class when multiple JAR files are provided to the class loader`() {
        useTemporaryFile("jar-with-single-class.jar", "jar-with-two-classes.jar") {
            val classLoader = SourceClassLoader(classResolver, UserPathSource(this))
            assertThrows<ClassNotFoundException> {
                classLoader.loadClassHeader("com.somewhere.NonExistentClass")
            }
        }
    }

    @Test
    fun `can load class when folder containing JAR file is provided to the class loader`() {
        useTemporaryFile("jar-with-single-class.jar", "jar-with-two-classes.jar") {
            val (first, second) = this
            val directory = first.parent
            UserPathSource(listOf(directory)).use { userPathSource ->
                val classLoader = SourceClassLoader(classResolver, userPathSource)
                assertThat(classLoader.getURLs()).anySatisfy {
                    assertThat(it).isEqualTo(first.toUri().toURL())
                }.anySatisfy {
                    assertThat(it).isEqualTo(second.toUri().toURL())
                }
            }
        }
    }

    @Test
    fun `can load source class that is split across parent and child loaders`() {
        UserPathSource(arrayOf(Action::class.java.protectionDomain.codeSource.location)).use { parentSource ->
            val parentLoader = SourceClassLoader(classResolver, parentSource)

            UserPathSource(arrayOf(ExampleAction::class.java.protectionDomain.codeSource.location)).use { childSource ->
                val childLoader = SourceClassLoader(classResolver, childSource, null, parentLoader)

                // Check that parent and child have different source locations.
                assertThat(parentLoader.getURLs())
                        .doesNotContainAnyElementsOf(childLoader.getURLs().toList())

                // Check that loading child with parent succeeds.
                assertNotNull(childLoader.loadClassHeader(ExampleAction::class.java.name))

                // Check that loading child without parent does fail.
                val orphanLoader = SourceClassLoader(classResolver, childSource)
                assertThrows<NoClassDefFoundError> { orphanLoader.loadClassHeader(ExampleAction::class.java.name) }
            }
        }
    }

    @Test
    fun `test interfaces are assignable to interfaces`() {
        UserPathSource(arrayOf(Action::class.java.protectionDomain.codeSource.location)).use { parentSource ->
            val parentLoader = SourceClassLoader(classResolver, parentSource)

            val map = parentLoader.loadClassHeader("java.util.Map")
            assertTrue(map.isInterface)

            val concurrentMap = parentLoader.loadClassHeader("java.util.concurrent.ConcurrentMap")
            assertTrue(concurrentMap.isInterface)

            assertTrue(map.isAssignableFrom(map))
            assertTrue(map.isAssignableFrom(concurrentMap))
            assertTrue(concurrentMap.isAssignableFrom(concurrentMap))
            assertFalse(concurrentMap.isAssignableFrom(map))
        }
    }

    @Test
    fun `test classes are assignable to classes`() {
        UserPathSource(arrayOf(Action::class.java.protectionDomain.codeSource.location)).use { parentSource ->
            val parentLoader = SourceClassLoader(classResolver, parentSource)

            val abstractMap = parentLoader.loadClassHeader("java.util.AbstractMap")
            assertFalse(abstractMap.isInterface)

            val concurrentHashMap = parentLoader.loadClassHeader("java.util.concurrent.ConcurrentHashMap")
            assertFalse(concurrentHashMap.isInterface)

            assertTrue(abstractMap.isAssignableFrom(abstractMap))
            assertTrue(abstractMap.isAssignableFrom(concurrentHashMap))
            assertTrue(concurrentHashMap.isAssignableFrom(concurrentHashMap))
            assertFalse(concurrentHashMap.isAssignableFrom(abstractMap))
        }
    }

    @Test
    fun `test classes are assignable to interfaces`() {
        UserPathSource(arrayOf(Action::class.java.protectionDomain.codeSource.location)).use { parentSource ->
            val parentLoader = SourceClassLoader(classResolver, parentSource)

            val serializable = parentLoader.loadClassHeader("java.io.Serializable")
            assertTrue(serializable.isInterface)

            val concurrentHashMap = parentLoader.loadClassHeader("java.util.concurrent.ConcurrentHashMap")
            assertFalse(concurrentHashMap.isInterface)

            assertTrue(serializable.isAssignableFrom(concurrentHashMap))
            assertFalse(concurrentHashMap.isAssignableFrom(serializable))
        }
    }

    @Test
    fun `test classes are assignable to superinterfaces`() {
        UserPathSource(arrayOf(Action::class.java.protectionDomain.codeSource.location)).use { parentSource ->
            val parentLoader = SourceClassLoader(classResolver, parentSource)

            val iterable = parentLoader.loadClassHeader("java.lang.Iterable")
            assertTrue(iterable.isInterface)

            val linkedHashSet = parentLoader.loadClassHeader("java.util.LinkedHashSet")
            assertFalse(linkedHashSet.isInterface)
            assertTrue(iterable.isAssignableFrom(linkedHashSet))
        }
    }

    @Test
    fun `test loading throwables`() {
        UserPathSource(arrayOf(Action::class.java.protectionDomain.codeSource.location)).use { parentSource ->
            val parentLoader = SourceClassLoader(classResolver, parentSource)
            val serializable = parentLoader.loadClassHeader("java.io.Serializable")

            val throwable = parentLoader.loadClassHeader("java.lang.Throwable")
            assertTrue(throwable.isThrowable)
            assertTrue(serializable.isAssignableFrom(throwable))

            val exception = parentLoader.loadClassHeader("java.lang.Exception")
            assertTrue(exception.isThrowable)
            assertTrue(serializable.isAssignableFrom(exception))

            val runtimeException = parentLoader.loadClassHeader("java.lang.RuntimeException")
            assertTrue(runtimeException.isThrowable)
            assertTrue(serializable.isAssignableFrom(runtimeException))

            val error = parentLoader.loadClassHeader("java.lang.Error")
            assertTrue(error.isThrowable)
            assertTrue(serializable.isAssignableFrom(error))

            val baseObject = parentLoader.loadClassHeader("java.lang.Object")
            assertFalse(baseObject.isThrowable)
            assertFalse(serializable.isAssignableFrom(baseObject))
        }
    }

    @ParameterizedTest
    @CsvSource(
        "META-INF/MANIFEST.MF,1",
        "java/nio/file/Paths.class,0",
        "sun/misc/Timer.class,0",
        "net/corda/djvm/Action.class,1",
        "net/corda/djvm/source/ExampleAction.class,1"
    )
    fun `test getting resources`(resourceName: String, expected: Int) {
        UserPathSource(arrayOf(Action::class.java.protectionDomain.codeSource.location)).use { parentSource ->
            val parentLoader = SourceClassLoader(classResolver, parentSource, apiSource)

            UserPathSource(arrayOf(ExampleAction::class.java.protectionDomain.codeSource.location)).use { childSource ->
                val childLoader = SourceClassLoader(
                    classResolver,
                    childSource,
                    null,
                    parentLoader
                )

                val resources = childLoader.getResources(resourceName).toList()
                assertEquals(expected, resources.size)
            }
        }
    }

    @Test
    fun `test getting all URLs`() {
        UserPathSource(arrayOf(Action::class.java.protectionDomain.codeSource.location)).use { parentSource ->
            val parentLoader = SourceClassLoader(classResolver, parentSource, apiSource)
            val urlsForParent = parentLoader.getAllURLs()
            assertEquals(2, urlsForParent.size)
            assertThat(urlsForParent).containsExactly(*parentLoader.getURLs())

            UserPathSource(arrayOf(ExampleAction::class.java.protectionDomain.codeSource.location)).use { childSource ->
                val childLoader = SourceClassLoader(
                    classResolver,
                    childSource,
                    null,
                    parentLoader
                )
                val urlsForChild = childLoader.getAllURLs()
                assertEquals(3, urlsForChild.size)
                assertThat(urlsForChild).containsExactlyInAnyOrder(
                    *parentLoader.getURLs() + childLoader.getURLs()
                )
            }
        }
    }

    @AfterEach
    fun cleanup() {
        apiSource.use {
            openedFiles.forEach {
                try {
                    Files.deleteIfExists(it)
                } catch (exception: Exception) {
                    // Ignore
                }
            }
        }
    }

    private val openedFiles = mutableListOf<Path>()

    private fun useTemporaryFile(vararg resourceNames: String, action: List<Path>.() -> Unit) {
        val paths = resourceNames.map { resourceName ->
            val stream = SourceClassLoaderTest::class.java.getResourceAsStream("/$resourceName")
                    ?: throw Exception("Cannot find resource \"$resourceName\"")
            Files.createTempFile("source-class-loader", ".jar").apply {
                Files.newOutputStream(this).use {
                    stream.copyTo(it)
                }
            }
        }
        openedFiles.addAll(paths)
        action(paths)
    }

}