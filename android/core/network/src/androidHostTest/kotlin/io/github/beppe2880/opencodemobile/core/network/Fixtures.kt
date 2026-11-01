package io.github.beppe2880.opencodemobile.core.network

import io.github.beppe2880.opencodemobile.api.models.OpenCodeEvent
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking

/** Sanitized responses recorded from OpenCode 2.0.27 (`shell_running_*`: 2.2.1), shared through core:testing resources. */
internal object Fixtures {
    val json = OpenCodeJson

    fun text(name: String): String =
        requireNotNull(javaClass.classLoader?.getResource(name)) { "Missing fixture $name" }.readText()

    inline fun <reified T> decode(name: String): T = json.decodeFromString(text(name))

    /** Every payload of [name], in order, including types this client cannot decode. */
    fun ssePayloads(name: String = "events.sse"): List<String> {
        val reader = SseReader(ByteReadChannel(text(name)))
        return runBlocking {
            buildList {
                while (true) add(reader.next() ?: break)
            }
        }
    }

    fun events(name: String = "events.sse"): List<OpenCodeEvent> = ssePayloads(name).mapNotNull(::decodeEvent)
}
