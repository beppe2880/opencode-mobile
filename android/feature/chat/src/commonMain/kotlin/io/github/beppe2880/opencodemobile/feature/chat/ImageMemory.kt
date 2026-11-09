package io.github.beppe2880.opencodemobile.feature.chat

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * Prompt images and tool files fetched again from the server (the message cache drops their
 * payload), kept while the app runs (one instance per app) so thumbnails do not reload on every
 * chat switch. Keys are message ids, or [toolMediaKey] for the files of one tool call.
 * Bounded by entry count: one entry can hold a few megabytes of base64.
 */
class ImageMemory(private val maxMessages: Int = 24) {

    private val lock = SynchronizedObject()

    // Least recently used first: reads move an entry to the end, the head is evicted.
    private val images = LinkedHashMap<String, List<String?>>()

    fun get(messageId: String): List<String?>? = synchronized(lock) {
        images.remove(messageId)?.also { images[messageId] = it }
    }

    fun put(messageId: String, dataUris: List<String?>) {
        synchronized(lock) {
            images.remove(messageId)
            images[messageId] = dataUris
            if (images.size > maxMessages) images.remove(images.keys.first())
        }
    }
}
