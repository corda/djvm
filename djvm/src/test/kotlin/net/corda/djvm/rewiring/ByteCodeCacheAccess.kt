@file:JvmName("ByteCodeCacheAccess")
package net.corda.djvm.rewiring

/**
 * Expose this function for testing only.
 */
fun ByteCodeCache.flush() = clear()
