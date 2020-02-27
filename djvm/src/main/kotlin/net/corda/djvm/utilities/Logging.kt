@file:JvmName("Logging")
package net.corda.djvm.utilities

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Get logger for provided class [T].
 */
internal inline fun <reified T : Any> loggerFor(): Logger {
    return LoggerFactory.getLogger(T::class.java)
}
