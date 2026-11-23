package io.github.beppe2880.opencodemobile.core.network

import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.HttpMethod
import io.ktor.http.encodedPath

/** Unlike admitted prompts, /generate returns no bytes until the model has finished. */
internal val SessionGenerationTimeouts = createClientPlugin("SessionGenerationTimeouts") {
    onRequest { request, _ -> request.applySessionGenerationTimeout() }
}

private val generationPath = Regex(".*/api/session/[^/]+/generate")

internal fun HttpRequestBuilder.applySessionGenerationTimeout() {
    if (method != HttpMethod.Post || !generationPath.matches(url.encodedPath)) return
    timeout {
        requestTimeoutMillis = 300_000
        socketTimeoutMillis = 300_000
    }
}
