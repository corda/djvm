package net.corda.djvm.analysis

import java.io.FileNotFoundException
import java.io.InputStream
import java.io.PushbackInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.GZIPInputStream

/**
 * Representation of a whitelist deciding what classes, interfaces and members are permissible and consequently can be
 * referenced from sandboxed code.
 *
 * @property namespace If provided, this parameter bounds the namespace of the whitelist.
 * @property entries A set of regular expressions used to determine whether a name is covered by the whitelist or not.
 * @property textEntries A set of textual entries used to determine whether a name is covered by the whitelist or not.
 */
open class Whitelist private constructor(
        private val namespace: Whitelist? = null,
        private val entries: Set<Regex>,
        private val textEntries: Set<String>
) {

    /**
     * Set of seen names that matched with the whitelist.
     */
    private val seenNames = mutableSetOf<String>()

    /**
     * Check if name falls within the namespace of the whitelist.
     */
    fun inNamespace(name: String): Boolean {
        return namespace != null && namespace.matches(name)
    }

    /**
     * Check if a name is covered by the whitelist.
     */
    fun matches(name: String): Boolean {
        if (name in seenNames) {
            return true
        }
        return when {
            name in textEntries -> {
                seenNames.add(name)
                true
            }
            entries.any { it.matches(name) } -> {
                seenNames.add(name)
                true
            }
            else -> false
        }
    }

    /**
     * Combine two whitelists into one.
     */
    operator fun plus(whitelist: Whitelist): Whitelist {
        val entries = entries + whitelist.entries
        val textEntries = textEntries + whitelist.textEntries
        return when {
            namespace != null && whitelist.namespace != null ->
                Whitelist(namespace + whitelist.namespace, entries, textEntries)
            namespace != null ->
                Whitelist(namespace, entries, textEntries)
            whitelist.namespace != null ->
                Whitelist(whitelist.namespace, entries, textEntries)
            else ->
                Whitelist(null, entries, textEntries)
        }
    }

    /**
     * Get a derived whitelist by adding a set of additional entries.
     */
    operator fun plus(additionalEntries: Set<Regex>): Whitelist {
        return plus(Whitelist(null, additionalEntries, emptySet()))
    }

    fun addTextEntries(additionalEntries: Set<String>): Whitelist {
        return plus(Whitelist(null, emptySet(), additionalEntries))
    }

    /**
     * Get a derived whitelist by adding an additional entry.
     */
    operator fun plus(additionalEntry: Regex): Whitelist {
        return plus(setOf(additionalEntry))
    }

    /**
     * Enumerate all the entries of the whitelist.
     */
    val items: Set<String>
        get() = textEntries + entries.map(Regex::pattern)

    companion object {
        private val everythingRegex = setOf(".*".toRegex())

        private val minimumSet = setOf(
            "^java/lang/AutoCloseable(\\..*)?\$".toRegex(),
            "^java/lang/Class(\\..*)?\$".toRegex(),
            "^java/lang/ClassLoader(\\..*)?\$".toRegex(),
            "^java/lang/Cloneable\$".toRegex(),
            "^java/lang/FunctionalInterface\$".toRegex(),
            "^java/lang/Object(\\..*)?\$".toRegex(),
            "^java/lang/StrictMath\\.(?!random:).*\$".toRegex(),
            "^java/lang/Void\$".toRegex(),
            "^java/lang/annotation/(Annotation|Documented|Inherited|Repeatable)\$".toRegex(),
            "^java/lang/invoke/LambdaMetafactory\$".toRegex(),
            "^java/lang/reflect/Array(\\..*)?\$".toRegex(),
            "^java/lang/reflect/Constructor(\\..*)?\$".toRegex(),
            "^java/io/Closeable(\\..*)?\$".toRegex(),
            "^java/io/Serializable\$".toRegex(),
            "^java/security/AccessController\\.doPrivileged(WithCombiner)?:\\(Ljava/security/Privileged(Exception)?Action;\\).*\$".toRegex(),
            "^java/util/concurrent/atomic/Atomic(Integer|Long|Reference)FieldUpdater\\.newUpdater:.*\$".toRegex()
        )

        /**
         * Empty whitelist.
         */
        @JvmField
        val EMPTY: Whitelist = Whitelist(null, emptySet(), emptySet())

        /**
         * The minimum set of classes that needs to be whitelisted from standard Java libraries.
         */
        @JvmField
        val MINIMAL: Whitelist = Whitelist(Whitelist(null, minimumSet, emptySet()), minimumSet, emptySet())

        /**
         * Whitelist everything.
         */
        @JvmField
        val EVERYTHING: Whitelist = Whitelist(
                Whitelist(null, everythingRegex, emptySet()),
                everythingRegex,
                emptySet()
        )

        /**
         * Load a whitelist from a resource stream.
         */
        fun fromResource(resourceName: String): Whitelist {
            val inputStream = Whitelist::class.java.getResourceAsStream("/$resourceName")
                    ?: throw FileNotFoundException("Cannot find resource \"$resourceName\"")
            return fromStream(inputStream)
        }

        /**
         * Load a whitelist from a file.
         */
        fun fromFile(file: Path): Whitelist {
            return Files.newInputStream(file).use(this::fromStream)
        }

        /**
         * Load a whitelist from a GZIP'ed or raw input stream.
         */
        fun fromStream(inputStream: InputStream): Whitelist {
            val namespaceEntries = mutableSetOf<Regex>()
            val entries = mutableSetOf<String>()
            decompressStream(inputStream).bufferedReader().use {
                var isCollectingFilterEntries = false
                for (line in it.lines().filter(String::isNotBlank)) {
                    when {
                        line.trim() == SECTION_SEPARATOR -> {
                            isCollectingFilterEntries = true
                        }
                        isCollectingFilterEntries -> entries.add(line)
                        else -> namespaceEntries.add(Regex(line))
                    }
                }
            }
            val namespace = if (namespaceEntries.isNotEmpty()) {
                Whitelist(null, namespaceEntries, emptySet())
            } else {
                null
            }
            return Whitelist(namespace = namespace, entries = emptySet(), textEntries = entries)
        }

        /**
         * Decompress stream if GZIP'ed, otherwise, use the raw stream.
         */
        private fun decompressStream(inputStream: InputStream): InputStream {
            val rawStream = PushbackInputStream(inputStream, 2)
            val signature = ByteArray(2)
            val length = rawStream.read(signature)
            rawStream.unread(signature, 0, length)
            return if (signature[0] == GZIP_MAGIC_FIRST_BYTE && signature[1] == GZIP_MAGIC_SECOND_BYTE) {
                GZIPInputStream(rawStream)
            } else {
                rawStream
            }
        }

        private const val SECTION_SEPARATOR = "---"
        private const val GZIP_MAGIC_FIRST_BYTE = GZIPInputStream.GZIP_MAGIC.toByte()
        private const val GZIP_MAGIC_SECOND_BYTE = (GZIPInputStream.GZIP_MAGIC shr 8).toByte()
    }

}

