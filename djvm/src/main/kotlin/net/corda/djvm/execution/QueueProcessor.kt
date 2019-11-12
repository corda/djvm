package net.corda.djvm.execution

import net.corda.djvm.utilities.loggerFor
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.function.BiConsumer

/**
 * Helper class for processing queued entities.
 */
class QueueProcessor<T>(
        private val deduplicationKeyExtractor: (T) -> String,
        vararg elements: T
) {

    private val queue = ConcurrentLinkedQueue<T>(elements.toMutableList())

    private val seenElements = mutableSetOf<String>()

    /**
     * Add an element to the queue.
     */
    fun enqueue(element: T) {
        logger.trace("Enqueuing {}...", element)
        val key = deduplicationKeyExtractor(element)
        if (key !in seenElements) {
            queue.add(element)
            seenElements.add(key)
        } else {
            logger.trace("Skipped {} as it has already been processed", element)
        }
    }

    /**
     * Remove one element from the queue.
     */
    fun dequeue(): T = queue.remove().apply {
        logger.trace("Popping {} from the queue...", this)
    }

    /**
     * Check if queue is empty.
     */
    fun isNotEmpty() = queue.isNotEmpty()

    /**
     * Process the current queue with provided action per element.
     */
    fun process(action: BiConsumer<QueueProcessor<T>, T>) {
        while (isNotEmpty()) {
            val element = dequeue()
            action.accept(this, element)
        }
    }

    private val logger = loggerFor<QueueProcessor<T>>()

}