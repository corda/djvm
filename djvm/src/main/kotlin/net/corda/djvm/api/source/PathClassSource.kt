package net.corda.djvm.api.source

import net.corda.djvm.api.source.ClassSource.Companion.isClass
import net.corda.djvm.api.source.ClassSource.Companion.isJar
import net.corda.djvm.source.JarInputStreamIterator
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

/**
 * Source of compiled Java classes.
 *
 * @property path The path of a class source in the file system, a JAR or a class.
 */
class PathClassSource(
        private val path: Path
) {
    /**
     * If [path] is a class file, return a single-element iterator with the stream of said class. If [path] is a JAR
     * file, an iterator traversing over the stream for each class file in the JAR is returned.
     */
    val streamIterator: Iterator<InputStream> get() {
        return when {
            isClass(path) -> {
                listOf(Files.newInputStream(path)).iterator()
            }
            isJar(path) -> {
                JarInputStreamIterator(Files.newInputStream(path))
            }
            else -> {
                throw IllegalArgumentException("Invalid file extension '$path'")
            }
        }
    }

}
