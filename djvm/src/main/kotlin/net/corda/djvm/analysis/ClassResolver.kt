package net.corda.djvm.analysis

import net.corda.djvm.code.impl.asPackagePath
import net.corda.djvm.code.impl.asResourcePath

/**
 * Functionality for resolving the class name of a sandboxable class.
 *
 * The resolution of a class name entails determining whether the class can be instrumented or not. This means that the
 * following criteria need to be satisfied:
 *  - The class does not reside in the "java/lang" package.
 *  - The class does not already reside in the top-level package named [sandboxPrefix].
 *
 * If these criteria have been satisfied, the fully-qualified class name will be derived by prepending [sandboxPrefix]
 * to it. Note that [ClassLoader] will not allow defining a class in a package whose fully-qualified class name starts
 * with "java/". That will result in the class loader throwing [SecurityException]. Also, some values map onto types
 * defined in "java/lang/", e.g., [Integer] and [String]. These cannot be trivially moved into a different package due
 * to the internal mechanisms of the JVM.
 *
 * @property templateClasses The set of classes that have been hand-rewritten for use inside the sandbox.
 * @property whitelist The set of classes in the Java runtime libraries that have been whitelisted and that should be
 * left alone.
 * @property sandboxPrefix The package name prefix to use for classes loaded into a sandbox.
 */
class ClassResolver(
    private val templateClasses: Set<String>,
    private val whitelist: Whitelist,
    private val sandboxPrefix: String
) {

    /**
     * Resolve the class name from a fully qualified name.
     */
    fun resolve(name: String): String {
        return when {
            name.startsWith('[') ->
                if (name.endsWith(';') ) {
                    complexArrayTypeRegex.replace(name) {
                        "${it.groupValues[1]}L${resolveName(it.groupValues[2])};"
                    }
                } else {
                    name
                }
            else -> resolveName(name)
        }
    }

    /**
     * Resolve the class name from a fully qualified normalized name.
     */
    fun resolveNormalized(name: String): String {
        return resolve(name.asResourcePath).asPackagePath
    }

    /**
     * Derive descriptor by resolving all referenced class names.
     */
    fun resolveDescriptor(descriptor: String): String {
        val outputDescriptor = StringBuilder()
        var longName = StringBuilder()
        var isProcessingLongName = false
        loop@ for (char in descriptor) {
            when {
                char != ';' && isProcessingLongName -> {
                    longName.append(char)
                    continue@loop
                }
                char == 'L' -> {
                    isProcessingLongName = true
                    longName = StringBuilder()
                }
                char == ';' -> {
                    outputDescriptor.append(resolve(longName.toString()))
                    isProcessingLongName = false
                }
            }
            outputDescriptor.append(char)
        }
        return outputDescriptor.toString()
    }

    /**
     * Reverse the resolution of a class name.
     * Does not work for array classes.
     */
    fun reverse(resolvedClassName: String): String {
        return if (isTemplateClass(resolvedClassName)) {
            resolvedClassName
        } else {
            removeSandboxPrefix(resolvedClassName)
        }
    }

    /**
     * Reverse the resolution of a class name from a fully qualified normalized name.
     */
    fun reverseNormalized(className: String): String {
        return reverse(className.asResourcePath).asPackagePath
    }

    /**
     * Generates the equivalent class name outside the sandbox from a fully qualified normalized name.
     */
    fun toSourceNormalized(className: String): String {
        return toSource(className.asResourcePath).asPackagePath
    }

    /**
     * Resolve sandboxed class name from a fully qualified name.
     * Does not work for array classes.
     */
    private fun resolveName(name: String): String {
        return if (isWhitelistedClass(name) || isSandboxClass(name)) {
            name
        } else {
            "$sandboxPrefix$name"
        }
    }

    /**
     * Maps a class name to its equivalent class outside the sandbox.
     * Needed by [net.corda.djvm.source.SourceClassLoader].
     */
    private fun toSource(className: String): String {
        return removeSandboxPrefix(className)
    }

    private fun removeSandboxPrefix(className: String): String {
        if (className.startsWith(sandboxPrefix)) {
            val nameWithoutPrefix = className.drop(sandboxPrefix.length)
            if (resolve(nameWithoutPrefix) == className) {
                return nameWithoutPrefix
            }
        }
        return className
    }

    /**
     * Checks if this class is one of the hand-written ones
     * that will be mapped "as-is" into the sandbox.
     */
    fun isTemplateClass(internalName: String): Boolean = internalName in templateClasses

    /**
     * Checks if this class exists inside the sandbox.
     * Does not work for array classes.
     */
    fun isSandboxClass(internalName: String): Boolean = internalName.startsWith(sandboxPrefix)

    /**
     * Check if class is whitelisted.
     */
    fun isWhitelistedClass(internalName: String): Boolean = whitelist.matches(internalName)

    companion object {
        private val complexArrayTypeRegex = "^(\\[+)L(.*);\$".toRegex()
    }

}