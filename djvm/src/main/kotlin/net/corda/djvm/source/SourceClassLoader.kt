@file:JvmName("SourceClassLoaderTools")
package net.corda.djvm.source

import net.corda.djvm.analysis.AnalysisContext
import net.corda.djvm.analysis.ClassResolver
import net.corda.djvm.analysis.ExceptionResolver.Companion.getDJVMExceptionOwner
import net.corda.djvm.analysis.ExceptionResolver.Companion.isDJVMException
import net.corda.djvm.analysis.SourceLocation
import net.corda.djvm.code.asResourcePath
import net.corda.djvm.messages.Message
import net.corda.djvm.messages.Severity
import net.corda.djvm.rewiring.SandboxClassLoadingException
import net.corda.djvm.utilities.loggerFor
import org.objectweb.asm.ClassReader
import java.io.Closeable
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.streams.toList

/**
 * Base interface for API and user sources.
 */
interface Source : Closeable {
    fun findResource(name: String): URL?
}

/**
 * This is the contract that our source of Java APIs must satisfy.
 */
interface ApiSource : Source

/**
 * This is the contract that the source of the user's own code must satisfy.
 * It will almost certainly be a [ClassLoader] of some description.
 */
interface UserSource : Source {
    @Throws(ClassNotFoundException::class)
    fun loadClass(className: String): Class<*>

    fun getURLs(): Array<URL>
}

/**
 * Class loader to manage an optional JAR of replacement Java APIs.
 * @param bootstrapJar The location of the JAR containing the Java APIs.
 */
class BootstrapClassLoader(bootstrapJar: Path)
    : URLClassLoader(resolvePaths(listOf(bootstrapJar)), null), ApiSource {
    /**
     * Only search our own jars for the given resource.
     */
    override fun getResource(name: String): URL? = findResource(name)
}

/**
 * Just like a [BootstrapClassLoader] without a JAR inside,
 * except that it doesn't inherit from [URLClassLoader] either.
 */
object EmptyApi : ClassLoader(null), ApiSource {
    override fun findResource(name: String): URL? {
        return super.findResource(name)
    }
    override fun getResource(name: String): URL? = findResource(name)
    override fun close() {}
}

/**
 * A [URLClassLoader] containing the user's own code for the DJVM.
 * It is used mainly by the tests, but also by the CLI tool.
 * @param urls The URLs for the resources to scan.
 */
class UserPathSource(urls: Array<URL>) : URLClassLoader(urls, null), UserSource {
    /**
     * @param paths The directories and explicit JAR files to scan.
     */
    constructor(paths: List<Path>) : this(resolvePaths(paths))
}

/**
 * Customizable class loader that allows the user to specify explicitly additional JARs and directories to scan.
 *
 * @property classResolver The resolver to use to derive the original name of a requested class.
 * @property userSource The [UserSource] containing the user's own code.
 * @property bootstrap The [ApiSource] containing the Java APIs for the sandbox.
 */
class SourceClassLoader(
    private val classResolver: ClassResolver,
    private val userSource: UserSource,
    private val bootstrap: ApiSource? = null
) : ClassLoader(null), Closeable {
    private companion object {
        private val logger = loggerFor<SourceClassLoader>()
    }

    override fun close() {
        bootstrap.use {
           userSource.close()
        }
    }

    fun getURLs(): Array<URL> = userSource.getURLs()

    /**
     * Open a [ClassReader] for the provided class name.
     */
    fun classReader(
        className: String, context: AnalysisContext, origin: String? = null
    ): ClassReader {
        val originalName = classResolver.reverse(className.asResourcePath)

        fun throwClassLoadingError(): Nothing {
            context.messages.provisionalAdd(Message(
                message ="Class file not found; $originalName.class",
                severity = Severity.ERROR,
                location = SourceLocation(origin ?: "")
            ))
            throw SandboxClassLoadingException(context)
        }

        return try {
            logger.trace("Opening ClassReader for class {}...", originalName)
            getResourceAsStream("$originalName.class")?.use(::ClassReader) ?: run(::throwClassLoadingError)
        } catch (exception: IOException) {
            throwClassLoadingError()
        }
    }

    /**
     * Load the class with the specified binary name.
     */
    @Throws(ClassNotFoundException::class)
    fun loadSourceClass(name: String): Class<*> {
        logger.trace("Loading source class {}...", name)
        // We need the name of the equivalent class outside of the sandbox.
        // This class is expected to belong to the application classloader.
        val originalName = classResolver.toSourceNormalized(name).let { n ->
            // A synthetic exception should be mapped back to its
            // corresponding exception in the original hierarchy.
            if (isDJVMException(n)) {
                getDJVMExceptionOwner(n)
            } else {
                n
            }
        }
        return loadClass(originalName)
    }

    @Throws(ClassNotFoundException::class)
    override fun loadClass(name: String): Class<*> {
        return userSource.loadClass(name)
    }

    /**
     * First check the bootstrap classloader, if we have one.
     * Otherwise check our parent classloader, followed by
     * the user-supplied jars.
     */
    override fun getResource(name: String): URL? {
        if (bootstrap != null) {
            val resource = bootstrap.findResource(name)
            if (resource != null) {
                return resource
            } else if (isJvmInternal(name)) {
                logger.error("Denying request for actual {}", name)
                return null
            }
        } else if (isJvmInternal(name)) {
            /**
             * Without a special [ApiSource], we need
             * to fetch Java API classes from the JVM itself.
             */
            return getSystemClassLoader().getResource(name)
        }

        return userSource.findResource(name)
    }
}

private fun resolvePaths(paths: List<Path>): Array<URL> {
    return paths.map(::expandPath).flatMap {
        when {
            !Files.exists(it) -> throw FileNotFoundException("File not found; $it")
            Files.isDirectory(it) -> {
                listOf(it.toURL()) + Files.list(it).filter(::isJarFile).map { jar -> jar.toURL() }.toList()
            }
            Files.isReadable(it) && isJarFile(it) -> listOf(it.toURL())
            else -> throw IllegalArgumentException("Expected JAR or class file, but found $it")
        }
    }.toTypedArray()
}

private fun expandPath(path: Path): Path {
    val pathString = path.toString()
    if (pathString.startsWith("~/")) {
        return homeDirectory.resolve(pathString.removePrefix("~/"))
    }
    return path
}

private fun isJarFile(path: Path) = path.toString().endsWith(".jar", true)

private fun Path.toURL(): URL = this.toUri().toURL()

private val homeDirectory: Path
    get() = Paths.get(System.getProperty("user.home"))

/**
 * Does [name] exist within any of the packages reserved for Java itself?
 */
private fun isJvmInternal(name: String): Boolean = name.startsWith("java/")
        || name.startsWith("sun/")
        || name.startsWith("com/sun/")
        || isJavaxInternal(name)
        || name.startsWith("jdk/")

private fun isJavaxInternal(name: String): Boolean {
    return name.startsWith("javax/") && name.substring("javax/".length).run {
        startsWith("activation/")
            || startsWith("crypto/")
            || startsWith("security/")
            || startsWith("xml/")
    }
}
