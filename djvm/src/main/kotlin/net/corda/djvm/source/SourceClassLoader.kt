@file:JvmName("SourceClassLoaderTools")
package net.corda.djvm.source

import net.corda.djvm.analysis.AnalysisContext
import net.corda.djvm.analysis.ClassAndMemberVisitor.Companion.API_VERSION
import net.corda.djvm.analysis.ClassResolver
import net.corda.djvm.analysis.SyntheticResolver.Companion.getDJVMSyntheticOwner
import net.corda.djvm.analysis.SyntheticResolver.Companion.isDJVMSynthetic
import net.corda.djvm.analysis.SourceLocation
import net.corda.djvm.code.asPackagePath
import net.corda.djvm.code.asResourcePath
import net.corda.djvm.messages.Message
import net.corda.djvm.messages.Severity
import net.corda.djvm.rewiring.SandboxClassLoadingException
import net.corda.djvm.utilities.loggerFor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassReader.SKIP_FRAMES
import org.objectweb.asm.ClassVisitor
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.AccessController.doPrivileged
import java.security.PrivilegedActionException
import java.security.PrivilegedExceptionAction
import java.util.*
import java.util.Collections.emptyEnumeration
import java.util.Collections.unmodifiableSet
import kotlin.streams.toList

/**
 * Base interface for API and user sources.
 */
interface Source : AutoCloseable {
    fun findResource(name: String): URL?
    fun findResources(name: String): Enumeration<URL>
    fun getURLs(): Array<URL>
}

/**
 * This is the contract that our source of Java APIs must satisfy.
 */
interface ApiSource : Source

/**
 * This is the contract that the source of the user's own code must satisfy.
 * It will almost certainly be a [ClassLoader] of some description.
 */
interface UserSource : Source

/**
 * Class loader to manage an optional JAR of replacement Java APIs.
 * @param bootstrapJar The location of the JAR containing the Java APIs.
 */
class BootstrapClassLoader(bootstrapJar: Path)
    : URLClassLoader(resolvePaths(listOf(bootstrapJar)), null), ApiSource {
    /**
     * Only search our own jar for the given resource.
     */
    override fun getResource(name: String): URL? = findResource(name)

    /**
     * Only search our own jar for the given resources.
     */
    override fun getResources(name: String): Enumeration<URL> = findResources(name)
}

/**
 * Just like a [BootstrapClassLoader] without a JAR inside,
 * except that it doesn't inherit from [URLClassLoader] either.
 */
object EmptyApi : ClassLoader(null), ApiSource {
    private val empty = arrayOf<URL>()
    override fun getURLs(): Array<URL> {
        return empty
    }
    override fun findResource(name: String): URL? {
        return super.findResource(name)
    }
    override fun findResources(name: String): Enumeration<URL> {
        return super.findResources(name)
    }
    override fun getResource(name: String): URL? = findResource(name)
    override fun getResources(name: String): Enumeration<URL> = findResources(name)
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
    private val bootstrap: ApiSource?,
    parent: SourceClassLoader?
) : ClassLoader(parent) {
    private companion object {
        private val logger = loggerFor<SourceClassLoader>()
    }

    private val headers = mutableMapOf<String, ClassHeader>()

    // Java-friendly constructors
    constructor(classResolver: ClassResolver, userSource: UserSource, bootstrap: ApiSource?)
        : this(classResolver, userSource, bootstrap, null)
    constructor(classResolver: ClassResolver, userSource: UserSource)
        : this(classResolver, userSource, null, null)

    fun getURLs(): Array<URL> = userSource.getURLs() + (bootstrap?.getURLs() ?: emptyArray())

    fun getAllURLs(): Set<URL> {
        val urls = getURLs().mapTo(LinkedHashSet()) { it }
        var next = parent as? SourceClassLoader
        while (next != null) {
            Collections.addAll<URL>(urls, *next.getURLs())
            next = next.parent as? SourceClassLoader
        }
        return urls
    }

    /**
     * Immutable set of [CodeLocation] objects describing
     * our source classes' code-bases.
     */
    val codeLocations: Set<CodeLocation> = unmodifiableSet(
        getURLs().mapTo(LinkedHashSet(), ::CodeLocation)
    )

    /**
     * Open a [ClassReader] for the provided class name.
     */
    fun classReader(
        className: String, context: AnalysisContext, origin: String?
    ): ClassReader {
        val originalName = classResolver.reverse(className.asResourcePath)

        fun throwClassLoadingError(): Nothing {
            val message = "Class file not found: $originalName.class"
            context.messages.add(Message(
                message = message,
                severity = Severity.ERROR,
                location = SourceLocation.Builder(origin ?: "").build()
            ))
            throw SandboxClassLoadingException(message, context)
        }

        val resource = getResource("$originalName.class") ?: run(::throwClassLoadingError)
        return try {
            logger.trace("Opening ClassReader for class {}...", originalName)
            resource.openStream().use(::ClassReader)
        } catch (_: IOException) {
            throwClassLoadingError()
        }
    }

    /**
     * Load the class header with the specified binary name.
     */
    @Throws(ClassNotFoundException::class)
    fun loadSourceHeader(name: String): ClassHeader {
        logger.trace("Loading source class {}...", name)
        // We need the name of the equivalent class outside of the sandbox.
        // This class is expected to belong to the application classloader.
        val originalName = classResolver.toSourceNormalized(name).let { n ->
            // A synthetic DJVM class should be mapped back to its
            // corresponding class in the original hierarchy.
            if (isDJVMSynthetic(n)) {
                getDJVMSyntheticOwner(n)
            } else {
                n
            }
        }
        return loadClassHeader(originalName)
    }

    @Throws(ClassNotFoundException::class)
    fun loadClassHeader(name: String): ClassHeader {
        return try {
            doPrivileged(PrivilegedExceptionAction {
                loadClassHeader(name, name.asResourcePath)
            })
        } catch (e: PrivilegedActionException) {
            throw e.cause ?: e
        }
    }

    private fun loadClassHeader(name: String, internalName: String): ClassHeader {
        synchronized(getClassLoadingLock(name)) {
            return headers[internalName] ?: run {
                (parent as? SourceClassLoader)?.run {
                    try {
                        loadClassHeader(name, internalName)
                    } catch (_: ClassNotFoundException) {
                        null
                    }
                } ?: loadBootstrapClassHeader(name, internalName)
                  ?: findClassHeader(name, internalName)
            }
        }
    }

    private fun loadBootstrapClassHeader(name: String, internalName: String): ClassHeader? {
        return findBootstrapResource("$internalName.class")?.let {
            defineHeader(name, internalName, it)
        }
    }

    private fun findClassHeader(name: String, internalName: String): ClassHeader {
        val url = userSource.findResource("$internalName.class") ?: throw ClassNotFoundException(name)
        return defineHeader(name, internalName, url)
    }

    private fun defineHeader(name: String, internalName: String, url: URL): ClassHeader {
        val byteCode = try {
            url.openStream().use {
                it.readBytes()
            }
        } catch (e: IOException) {
            throw ClassNotFoundException(name, e)
        }
        return defineHeader(name, internalName, byteCode)
    }

    private fun defineHeader(name: String, internalName: String, byteCode: ByteArray): ClassHeader {
        val visitor = HeaderVisitor()
        ClassReader(byteCode).accept(visitor, SKIP_FRAMES)

        if (internalName != visitor.internalName) {
            throw NoClassDefFoundError(internalName)
        }

        return ClassHeader(
            classLoader = this,
            name = name,
            internalName = internalName,
            superclass = visitor.superName?.run {
                try {
                    loadClassHeader(asPackagePath, this)
                } catch (e: ClassNotFoundException) {
                    throw NoClassDefFoundError(e.message).apply { initCause(e) }
                }
            },
            interfaces = visitor.interfaces.mapTo(LinkedHashSet()) {
                try {
                    loadClassHeader(it.asPackagePath, it)
                } catch (e: ClassNotFoundException) {
                    throw NoClassDefFoundError(e.message).apply { initCause(e) }
                }
            },
            flags = visitor.access
        ).apply {
            headers[internalName] = this
        }
    }

    private fun findBootstrapResource(name: String): URL? {
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
            return getSystemResource(name)
        }
        return null
    }

    private fun findBootstrapResources(name: String): Enumeration<URL> {
        return bootstrap?.findResources(name)
            ?: if (isJvmInternal(name)) {
                /**
                 * Without a special [ApiSource], we need
                 * to fetch Java API classes from the JVM itself.
                 */
                getSystemResources(name)
            } else {
                emptyEnumeration()
            }
    }

    /**
     * First check the parent classloader, if we have one.
     * Otherwise check any bootstrap classloader, followed by
     * the user-supplied jars.
     */
    override fun getResource(name: String): URL? {
        return parent?.getResource(name) ?: findBootstrapResource(name) ?: userSource.findResource(name)
    }

    /**
     * Check the parent [SourceClassLoader] first, if we have one.
     * Otherwise check our [ApiSource], followed by any of our
     * own resources.
     */
    override fun getResources(name: String): Enumeration<URL> {
        val resources = mutableListOf<Enumeration<URL>>()
        resources.add(parent?.getResources(name) ?: findBootstrapResources(name))
        resources.add(userSource.findResources(name))
        return CompoundEnumeration(resources)
    }

    /**
     * Internal [ClassVisitor] that reads only a class's superclass
     * and the interfaces that it implements. This allows us to
     * implement just enough reflection-like functionality for ASM's
     * common superclass algorithm without loading these classes.
     */
    private class HeaderVisitor : ClassVisitor(API_VERSION) {
        var access: Int = 0
        var internalName: String = ""
        var superName: String? = null
        val interfaces = mutableListOf<String>()

        override fun visit(
            version: Int,
            access: Int,
            name: String,
            signature: String?,
            superName: String?,
            interfaces: Array<String>?
        ) {
            this.access = access
            this.internalName = name
            this.superName = superName
            Collections.addAll(this.interfaces, *interfaces ?: emptyArray())
        }
    }
}

private fun resolvePaths(paths: List<Path>): Array<URL> {
    return paths.map(::expandPath).flatMap {
        when {
            !Files.exists(it) -> throw FileNotFoundException("File not found; $it")
            Files.isDirectory(it) -> {
                listOf(it.toURL()) + Files.list(it).use { files ->
                    files.filter(::isJarFile).map(Path::toURL).toList()
                }
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

private fun isJarFile(path: Path): Boolean = path.toString().endsWith(".jar", true)

private fun Path.toURL(): URL = toUri().toURL()

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

/**
 * Rewrite this class because it has been removed from Java 9+.
 */
private class CompoundEnumeration<E>(private val enums: List<Enumeration<E>>) : Enumeration<E> {
    private var index: Int = 0

    override fun hasMoreElements(): Boolean {
        while (index < enums.size) {
            if (enums[index].hasMoreElements()) {
                return true
            }
            ++index
        }
        return false
    }

    override fun nextElement(): E {
        if (hasMoreElements()) {
            return enums[index].nextElement()
        }
        throw NoSuchElementException()
    }
}
