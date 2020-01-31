@file:JvmName("ByteCodeCacheAccess")
package net.corda.djvm.rewiring

/**
 * Expose this function for testing only.
 */
fun ByteCodeCache.flush() = clear()

fun ByteCodeCache.flushAll() {
    var cache = this
    do {
        cache.flush()
        cache = cache.parent ?: break
    } while (true)
}