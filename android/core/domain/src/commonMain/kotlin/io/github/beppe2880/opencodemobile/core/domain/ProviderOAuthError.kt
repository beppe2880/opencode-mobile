package io.github.beppe2880.opencodemobile.core.domain

/** Only fixed diagnostic identifiers and HTTP status cross into UI; never retain a raw error or cause. */
class ProviderOAuthError(val stage: ProviderOAuthStage, httpStatus: Int? = null) : OpenCodeError("Browser OAuth failed") {
    val httpStatus: Int? = httpStatus?.takeIf { it in 100..599 }
    val diagnosticCode: String get() = "OAUTH_${stage.name}" + (httpStatus?.let { "_HTTP_$it" } ?: "")
}

enum class ProviderOAuthStage {
    Preparation,
    Callback,
    TokenExchange,
    Scope,
    TokenResponse,
    Jwks,
    Signature,
    Claims,
    ServerChanged,
    Import,
    Expired,
    Unknown,
}
