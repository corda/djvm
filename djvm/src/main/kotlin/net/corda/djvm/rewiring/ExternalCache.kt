@file:JvmName("ExternalCache")
package net.corda.djvm.rewiring

import java.util.concurrent.ConcurrentMap

typealias ExternalCache = ConcurrentMap<ByteCodeKey, ByteCode>
