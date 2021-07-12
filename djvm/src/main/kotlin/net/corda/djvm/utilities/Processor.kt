@file:JvmName("Processor")
package net.corda.djvm.utilities

import net.corda.djvm.analysis.SourceLocation
import net.corda.djvm.messages.Message
import net.corda.djvm.messages.MessageCollection
import java.util.function.Consumer
import java.util.function.Predicate

/**
 * Utility for processing a set of entries in a list matching a particular type.
 */

/**
 * Process entries of type [T] in the provided list, using a guard around the processing of each item, catching
 * any [Message] that might get raised.
 */
inline fun <reified T> processEntriesOfType(
    list: List<T>,
    messages: MessageCollection,
    processor: Consumer<T>
) {
    for (item in list) {
        try {
            processor.accept(item)
        } catch (exception: Throwable) {
            val location = SourceLocation.Builder(item.toString()).build()
            messages.add(Message.fromThrowable(exception, location))
        }
    }
}
