package io.github.beppe2880.opencodemobile.feature.settings.usage

import androidx.lifecycle.viewModelScope
import io.github.beppe2880.opencodemobile.core.domain.CatalogRepository
import io.github.beppe2880.opencodemobile.core.domain.SessionRepository
import io.github.beppe2880.opencodemobile.core.mvi.BaseViewModel
import io.github.beppe2880.opencodemobile.core.ui.text.toUiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

class UsageViewModel(
    private val sessions: SessionRepository,
    private val catalog: CatalogRepository,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
) : BaseViewModel<UsageState, UsageEvent, UsageEffect>(UsageState()) {

    private var loadJob: Job? = null

    init {
        load()
        viewModelScope.launch {
            catalog.models(null).onSuccess { models ->
                updateState { s -> s.copy(modelNames = models.associate { it.ref.key to ModelName(it.name, it.providerName) }) }
            }
        }
    }

    override fun onEvent(event: UsageEvent) {
        when (event) {
            is UsageEvent.SelectPeriod -> if (event.period != currentState.period) {
                updateState { it.copy(period = event.period) }
                load()
            }
            UsageEvent.Retry -> load()
            UsageEvent.Back -> sendEffect(UsageEffect.NavigateBack)
        }
    }

    /** From the start of the first local day of the period until now. */
    private fun load() {
        val period = currentState.period
        val now = clock()
        val zoneId = zone()
        val from = Instant.ofEpochMilli(now).atZone(zoneId).toLocalDate()
            .minusDays(period.days - 1L)
            .atStartOfDay(zoneId).toInstant().toEpochMilli()
        loadJob?.cancel()
        updateState { it.copy(loading = true, error = null) }
        loadJob = viewModelScope.launch {
            sessions.usage(from, now, zoneId.id).fold(
                onSuccess = { stats -> updateState { it.copy(stats = stats, loading = false) } },
                onFailure = { error -> updateState { it.copy(loading = false, error = error.toUiText()) } },
            )
        }
    }
}
