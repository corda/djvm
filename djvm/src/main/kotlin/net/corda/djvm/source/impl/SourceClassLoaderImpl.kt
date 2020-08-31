@file:JvmName("SourceClassLoaderTools")
package net.corda.djvm.source.impl

import net.corda.djvm.analysis.AnalysisContext
import net.corda.djvm.analysis.ClassResolver
import net.corda.djvm.analysis.SourceLocation
import net.corda.djvm.analysis.SyntheticResolver.Companion.getDJVMSyntheticOwner
import net.corda.djvm.analysis.SyntheticResolver.Companion.isDJVMSynthetic
import net.corda.djvm.analysis.impl.ClassAndMemberVisitor.Companion.API_VERSION
import net.corda.djvm.code.asPackagePath
import net.corda.djvm.code.asResourcePath
import net.corda.djvm.messages.Message
import net.corda.djvm.messages.Severity
import net.corda.djvm.rewiring.SandboxClassLoadingException
import net.corda.djvm.source.ApiSource
import net.corda.djvm.source.ClassHeader
import net.corda.djvm.source.CodeLocation
import net.corda.djvm.source.SourceClassLoader
import net.corda.djvm.source.UserSource
import net.corda.djvm.utilities.loggerFor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassReader.SKIP_CODE
import org.objectweb.asm.ClassReader.SKIP_DEBUG
import org.objectweb.asm.ClassReader.SKIP_FRAMES
import org.objectweb.asm.ClassVisitor
import java.io.IOException
import java.net.URL
import java.security.AccessController.doPrivileged
import java.security.PrivilegedActionException
import java.security.PrivilegedExceptionAction
import java.util.*
import java.util.Collections.emptyEnumeration
import java.util.Collections.unmodifiableSet

/**
 * Customizable class loader that allows the user to specify explicitly additional JARs and directories to scan.
 *
 * @property classResolver The resolver to use to derive the original name of a requested class.
 * @property userSource The [UserSource] containing the user's own code.
 * @property bootstrap The [ApiSource] containing the Java APIs for the sandbox.
 */
class SourceClassLoaderImpl(
    private val classResolver: ClassResolver,
    private val userSource: UserSource,
    private val bootstrap: ApiSource?,
    parent: SourceClassLoader?
) : SourceClassLoader(parent) {
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

    override fun getAllURLs(): Set<URL> {
        val urls = getURLs().mapTo(LinkedHashSet()) { it }
        var next = parent as? SourceClassLoaderImpl
        while (next != null) {
            Collections.addAll(urls, *next.getURLs())
            next = next.parent as? SourceClassLoaderImpl
        }
        return urls
    }

    /**
     * Immutable set of [CodeLocation] objects describing
     * our source classes' code-bases.
     */
    override val codeLocations: Set<CodeLocation> = unmodifiableSet(
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
            resource.unversioned.openStream().use(::ClassReader)
        } catch (_: IOException) {
            throwClassLoadingError()
        }
    }

    /**
     * Load the underlying [ClassHeader] for the sandbox class
     * with the specified binary name.
     */
    @Throws(ClassNotFoundException::class)
    override fun loadSourceHeader(name: String): ClassHeader {
        logger.trace("Loading source class for {}...", name)
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

    /**
     * Load the [ClassHeader] for the source class
     * with the specified binary name.
     */
    @Throws(ClassNotFoundException::class)
    override fun loadClassHeader(name: String): ClassHeader {
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
                (parent as? SourceClassLoaderImpl)?.run {
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
            url.unversioned.openStream().use {
                it.readBytes()
            }
        } catch (e: IOException) {
            throw ClassNotFoundException(name, e)
        }
        return defineHeader(name, internalName, byteCode)
    }

    private fun defineHeader(name: String, internalName: String, byteCode: ByteArray): ClassHeader {
        val visitor = HeaderVisitor()
        ClassReader(byteCode).accept(visitor, SKIP_FRAMES or SKIP_DEBUG or SKIP_CODE)

        if (internalName != visitor.internalName) {
            throw NoClassDefFoundError(internalName)
        }

        return ClassHeaderImpl(
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

private val VERSIONED_JAR = "^(.*!/)META-INF/versions/\\d++/(.*)\$".toRegex()

/**
 * Converts a multi-release Jar URL into an ordinary Jar URL.
 */
val URL.unversioned: URL get() {
    return if (protocol == "jar") {
        VERSIONED_JAR.matchEntire(file)?.let {
            URL(protocol, host, port, it.groupValues[1] + it.groupValues[2])
        } ?: this
    } else {
        this
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
