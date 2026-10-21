package io.github.beppe2880.opencodemobile.feature.chat

/**
 * Prompt images fetched again from the server (the message cache drops their payload), kept
 * while the app runs (one instance per app) so thumbnails do not reload on every chat switch.
 * Bounded by message count: one entry can hold a few megabytes of base64.
 */
class ImageMemory(private val maxMessages: Int = 24) {

    private val images = object : LinkedHashMap<String, List<String?>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<String?>>) = size > maxMessages
    }

    @Synchronized
    fun get(messageId: String): List<String?>? = images[messageId]

    @Synchronized
    fun put(messageId: String, dataUris: List<String?>) {
        images[messageId] = dataUris
    }
}
