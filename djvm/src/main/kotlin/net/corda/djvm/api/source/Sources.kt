@file:JvmName("SourceTools")
package net.corda.djvm.api.source

import java.io.FileNotFoundException
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Enumeration
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
