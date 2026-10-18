package io.github.beppe2880.opencodemobile.core.network.mapping

import io.github.beppe2880.opencodemobile.api.models.SessionStatsInfo
import io.github.beppe2880.opencodemobile.core.domain.DailyActivity
import io.github.beppe2880.opencodemobile.core.domain.ModelUsage
import io.github.beppe2880.opencodemobile.core.domain.UsageStats

/** The server lists each model variant apart; they are merged into one entry per model. */
fun SessionStatsInfo.toDomain(): UsageStats = UsageStats(
    sessions = sessions,
    subagents = subagents,
    prompts = prompts,
    steps = steps,
    cost = cost,
    tokens = tokens.toDomain(),
    activeDays = activeDays,
    streak = streak,
    activity = activity.map { DailyActivity(it.date, it.steps) }.sortedBy { it.date },
    models = models
        .map { ModelUsage(it.model.toDomain().copy(variant = null), it.steps, it.cost, it.tokens.toDomain()) }
        .groupBy { it.model.key }
        .map { (_, same) ->
            same.reduce { a, b -> ModelUsage(a.model, a.steps + b.steps, a.cost + b.cost, a.tokens + b.tokens) }
        }
        .sortedWith(compareByDescending<ModelUsage> { it.cost }.thenByDescending { it.steps }),
)
