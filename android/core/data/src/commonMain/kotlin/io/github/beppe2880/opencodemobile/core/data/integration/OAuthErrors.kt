package io.github.beppe2880.opencodemobile.core.data.integration

import io.github.beppe2880.opencodemobile.core.domain.OpenCodeError
import io.github.beppe2880.opencodemobile.core.domain.ProviderOAuthError
import io.github.beppe2880.opencodemobile.core.domain.ProviderOAuthStage
import io.github.beppe2880.opencodemobile.core.domain.ProviderOAuthTransport
import kotlinx.coroutines.CancellationException

/** HTTP failure/invalid JSON. No body, URL or original exception is retained. */
internal class OAuthHttpError(val status: Int, val elapsedMillis: Long? = null) : Exception("OAuth HTTP response failed")

internal class OAuthTransportError(
    val kind: ProviderOAuthTransport,
    val elapsedMillis: Long,
    val httpStatus: Int? = null,
) : Exception("OAuth transport failed")

internal suspend fun <T> oauthStep(stage: ProviderOAuthStage, block: suspend () -> T): T = try {
    block()
} catch (error: CancellationException) {
    throw error
} catch (error: ProviderOAuthError) {
    throw error
} catch (error: Exception) {
    val status = when (error) {
        is OAuthHttpError -> error.status
        is OAuthTransportError -> error.httpStatus
        is OpenCodeError.Server -> error.status
        is OpenCodeError.Unauthorized -> error.httpStatus
        is OpenCodeError.NotFound -> error.httpStatus
        else -> null
    }
    val elapsed = when (error) {
        is OAuthHttpError -> error.elapsedMillis
        is OAuthTransportError -> error.elapsedMillis
        else -> null
    }
    throw ProviderOAuthError(stage, status, (error as? OAuthTransportError)?.kind, elapsed)
}
