package io.github.beppe2880.opencodemobile.feature.settings.usage

import androidx.compose.runtime.Immutable
import io.github.beppe2880.opencodemobile.core.domain.UsageStats
import io.github.beppe2880.opencodemobile.core.ui.text.UiText

/** Calendar days counted, today included. */
enum class UsagePeriod(val days: Int) { Today(1), Week(7), Month(30) }

/** Display names of a model of the usage list; raw ids when the model is not in the catalog. */
data class ModelName(val name: String, val provider: String)

@Immutable
data class UsageState(
    val period: UsagePeriod = UsagePeriod.Week,
    /** Kept while another period loads. */
    val stats: UsageStats? = null,
    val loading: Boolean = true,
    val error: UiText? = null,
    /** Catalog names by model key (`provider/id`). */
    val modelNames: Map<String, ModelName> = emptyMap(),
)

sealed interface UsageEvent {
    data class SelectPeriod(val period: UsagePeriod) : UsageEvent
    data object Retry : UsageEvent
    data object Back : UsageEvent
}

sealed interface UsageEffect {
    data object NavigateBack : UsageEffect
}
