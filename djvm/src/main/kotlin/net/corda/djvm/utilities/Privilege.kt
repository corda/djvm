@file:JvmName("Privilege")
package net.corda.djvm.utilities

import java.security.AccessController
import java.security.PrivilegedAction
import java.security.PrivilegedActionException
import java.security.PrivilegedExceptionAction

internal fun <T> doPrivileged(action: PrivilegedExceptionAction<T>): T {
    return try {
        AccessController.doPrivileged(action)
    } catch (e: PrivilegedActionException) {
        throw e.exception
    }
}

@Suppress("nothing_to_inline")
internal inline fun <T> doPrivileged(action: PrivilegedAction<T>): T {
    return AccessController.doPrivileged(action)
}
