package io.github.beppe2880.opencodemobile.core.domain

/** Server-discovered integration. No provider names or authentication methods are hard-coded. */
data class KeyIntegration(
    val id: String,
    val name: String,
    val connected: Boolean,
    val keyLabel: String?,
    val fields: List<FormField>,
    val hidden: Set<String> = emptySet(),
    val hasKey: Boolean = true,
    val oauthMethods: List<ProviderOAuthMethod> = emptyList(),
) {
    fun answers(values: Map<String, FormAnswer>): Map<String, FormAnswer> {
        val result = mutableMapOf<String, FormAnswer>()
        for (field in fields) {
            if (field.active(result)) {
                val answer = if (field.key in hidden) field.defaultAnswer() else values[field.key]
                if (answer != null) result[field.key] = answer
            }
        }
        return result
    }

    fun defaults(): Map<String, FormAnswer> = fields.mapNotNull { field -> field.defaultAnswer()?.let { field.key to it } }.toMap()

    fun visibleFields(values: Map<String, FormAnswer>): List<FormField> {
        val resolved = answers(values)
        return fields.filter { it.key !in hidden && it.active(resolved) }
    }

    fun isComplete(values: Map<String, FormAnswer>): Boolean = visibleFields(values).all { field ->
        when (val answer = values[field.key]) {
            null -> !field.required
            is FormAnswer.Text ->
                field is FormField.Text &&
                    (!field.required || answer.value.isNotBlank()) &&
                    (field.minLength == null || answer.value.length >= field.minLength) &&
                    (field.maxLength == null || answer.value.length <= field.maxLength) &&
                    (field.pattern == null || runCatching { Regex(field.pattern).containsMatchIn(answer.value) }.getOrDefault(false)) &&
                    (field.options.isEmpty() || field.custom || field.options.any { it.value == answer.value })
            is FormAnswer.Number -> field is FormField.Number && answer.value.isFinite() &&
                (!field.integer || answer.value % 1.0 == 0.0) &&
                (field.min == null || answer.value >= field.min) && (field.max == null || answer.value <= field.max)
            is FormAnswer.Choices ->
                field is FormField.MultiSelect &&
                    (!field.required || answer.values.isNotEmpty()) &&
                    (field.minItems == null || answer.values.size >= field.minItems) &&
                    (field.maxItems == null || answer.values.size <= field.maxItems)
            is FormAnswer.Bool -> true
        }
    }
}

private fun FormField.active(answers: Map<String, FormAnswer>): Boolean = conditions.all { condition ->
    val actual = answers[condition.key] ?: return@all false
    val equal = if (actual is FormAnswer.Choices && condition.value is FormAnswer.Text) condition.value.value in actual.values else actual == condition.value
    equal == condition.equals
}

interface IntegrationRepository {
    /** Structured local browser flow. Closing/cancelling the caller releases the listener and temporary secrets. */
    suspend fun connectBrowserOAuth(
        serverId: String,
        directory: String,
        integrationId: String,
        methodId: String,
        openBrowser: suspend (String) -> Unit,
    ): Result<Unit> = Result.failure(OpenCodeError.NotFound("Browser OAuth unavailable"))
    suspend fun startOAuth(
        serverId: String,
        directory: String,
        integrationId: String,
        methodId: String,
        answers: Map<String, FormAnswer>,
    ): Result<ProviderOAuthAttempt> =
        Result.failure(OpenCodeError.NotFound("OAuth unavailable"))
    suspend fun oauthStatus(serverId: String, directory: String, integrationId: String, attemptId: String): Result<ProviderOAuthStatus> =
        Result.failure(OpenCodeError.NotFound("OAuth unavailable"))
    suspend fun cancelOAuth(serverId: String, directory: String, integrationId: String, attemptId: String): Result<Unit> = Result.success(Unit)

    /** Includes every integration offering a key method, scoped to the original saved server. */
    suspend fun listKeys(serverId: String, directory: String): Result<List<KeyIntegration>>
    suspend fun connectKey(serverId: String, directory: String, integrationId: String, key: String, answers: Map<String, FormAnswer>): Result<Unit>
}

data class ProviderOAuthMethod(
    val id: String,
    val label: String,
    val form: KeyIntegration,
    val mobileSupported: Boolean,
    val browser: Boolean = false,
    val requiresSecureTransport: Boolean = false,
)
data class ProviderOAuthAttempt(val id: String, val url: String, val instructions: String)
enum class ProviderOAuthStatus { Pending, Complete, Failed, Expired }

/** Only audited remote-compatible flows. `auto` alone does not distinguish device from loopback OAuth. */
fun deviceOAuthSupported(integrationId: String, methodId: String): Boolean = when (integrationId) {
    "github-copilot", "opencode", "xai" -> methodId == "device"
    "openai" -> methodId == "chatgpt-headless"
    else -> false
}
