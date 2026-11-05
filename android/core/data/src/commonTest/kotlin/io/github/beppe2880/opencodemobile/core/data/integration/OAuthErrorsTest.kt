package io.github.beppe2880.opencodemobile.core.data.integration

import io.github.beppe2880.opencodemobile.core.domain.OpenCodeError
import io.github.beppe2880.opencodemobile.core.domain.ProviderOAuthError
import io.github.beppe2880.opencodemobile.core.domain.ProviderOAuthStage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

class OAuthErrorsTest {
    @Test fun serverErrorsExposeOnlyStageAndStatus() = runTest {
        val errors = listOf(
            OpenCodeError.Server(403, "secret-tag", "secret-token") to 403,
            OpenCodeError.Unauthorized("secret-token", httpStatus = 401) to 401,
            OpenCodeError.NotFound("secret-token", httpStatus = 404) to 404,
            OpenCodeError.Unauthorized("local missing credentials") to null,
            OpenCodeError.NotFound("local removed profile") to null,
            OpenCodeError.Unreachable("secret-token", IllegalStateException("secret-code")) to null,
        )
        errors.forEach { (raw, status) ->
            val failure = assertIs<ProviderOAuthError>(runCatching { oauthStep(ProviderOAuthStage.Import) { throw raw } }.exceptionOrNull())
            assertEquals(ProviderOAuthStage.Import, failure.stage)
            assertEquals(status, failure.httpStatus)
            assertNull(failure.cause)
            assertFalse(failure.toString().contains("secret"))
            assertFalse(failure.diagnosticCode.contains("secret"))
        }
    }

    @Test fun nestedStepsPreserveOriginalPhaseAndCancellation() = runTest {
        val original = ProviderOAuthError(ProviderOAuthStage.Claims)
        val failure = runCatching { oauthStep(ProviderOAuthStage.Preparation) { throw original } }.exceptionOrNull()
        assertSame(original, failure)
        val cancellation = CancellationException("cancelled")
        assertSame(cancellation, runCatching { oauthStep(ProviderOAuthStage.Callback) { throw cancellation } }.exceptionOrNull())
    }

    @Test fun unknownExceptionsAndInvalidStatusesCannotInjectDiagnostics() = runTest {
        val failure = assertIs<ProviderOAuthError>(
            runCatching {
                oauthStep(ProviderOAuthStage.TokenExchange) { throw IllegalStateException("https://secret.example?code=secret") }
            }.exceptionOrNull(),
        )
        assertEquals("OAUTH_TokenExchange", failure.diagnosticCode)
        assertNull(failure.cause)
        assertNull(ProviderOAuthError(ProviderOAuthStage.Import, 0).httpStatus)
    }
}
