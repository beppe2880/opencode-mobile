package io.github.beppe2880.opencodemobile.core.domain

/** [variant] is the reasoning level (`low`, `high`, …); null means the model's default. */
data class ModelRef(
    val id: String,
    val providerId: String,
    val variant: String? = null,
) {
    /** Identifies the model regardless of [variant]. */
    val key: String get() = "$providerId/$id"
}

/** Models of the most recently updated sessions, newest first, one entry per model. */
fun List<Session>.recentModels(limit: Int = RECENT_MODELS): List<ModelRef> =
    sortedByDescending { it.updatedAt }.mapNotNull { it.model }.distinctBy { it.key }.take(limit)

const val RECENT_MODELS = 5

data class TokenUsage(
    val input: Long = 0,
    val output: Long = 0,
    val reasoning: Long = 0,
    val cacheRead: Long = 0,
    val cacheWrite: Long = 0,
) {
    val total: Long get() = input + output + reasoning + cacheRead + cacheWrite

    operator fun plus(other: TokenUsage) = TokenUsage(
        input + other.input,
        output + other.output,
        reasoning + other.reasoning,
        cacheRead + other.cacheRead,
        cacheWrite + other.cacheWrite,
    )
}

/** Activity and spending of every session in a time range, as the server counts them. */
data class UsageStats(
    val sessions: Long,
    val subagents: Long,
    val prompts: Long,
    val steps: Long,
    val cost: Double,
    val tokens: TokenUsage,
    val activeDays: Long,
    val streak: Long,
    /** Steps per local day (`yyyy-MM-dd`), oldest first. */
    val activity: List<DailyActivity>,
    /** One entry per model (variants merged), most expensive first. */
    val models: List<ModelUsage>,
)

data class DailyActivity(val date: String, val steps: Long)

data class ModelUsage(val model: ModelRef, val steps: Long, val cost: Double, val tokens: TokenUsage)

enum class SessionOutcome { Succeeded, Failed, Interrupted }

data class Session(
    val id: String,
    val parentId: String?,
    val projectId: String,
    val title: String,
    val directory: String,
    val agent: String?,
    val model: ModelRef?,
    val cost: Double,
    val tokens: TokenUsage,
    val outcome: SessionOutcome?,
    val createdAt: Long,
    val updatedAt: Long,
    val archivedAt: Long?,
    /** First message of a staged revert: it and everything after are hidden until committed or cleared. */
    val revertMessageId: String? = null,
) {
    /** Last path segment of the working directory, used as a compact project label. */
    val directoryName: String
        get() = directory.trimEnd('/').substringAfterLast('/').ifEmpty { "/" }
}

data class Project(
    val id: String,
    val directory: String,
    val name: String?,
    val vcs: String?,
    val updatedAt: Long,
) {
    val displayName: String
        get() = name?.takeIf { it.isNotBlank() }
            ?: directory.trimEnd('/').substringAfterLast('/').ifEmpty { "/" }
}

data class Agent(
    val id: String,
    val name: String,
    val description: String?,
    val mode: AgentMode,
    val hidden: Boolean,
    val color: String?,
)

enum class AgentMode { Primary, Subagent, All }

data class ModelInfo(
    val ref: ModelRef,
    val name: String,
    val providerId: String,
    val family: String?,
    val contextLimit: Long,
    val supportsImages: Boolean,
    val variants: List<String>,
    val releasedAt: Long,
    val enabled: Boolean,
    val status: String,
    /** Display name of the provider, e.g. "Anthropic"; falls back to [providerId]. */
    val providerName: String = providerId,
)
