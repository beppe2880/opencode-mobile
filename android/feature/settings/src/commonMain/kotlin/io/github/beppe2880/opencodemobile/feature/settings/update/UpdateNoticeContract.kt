package io.github.beppe2880.opencodemobile.feature.settings.update

import androidx.compose.runtime.Immutable
import io.github.beppe2880.opencodemobile.core.domain.AppRelease

@Immutable
data class UpdateNoticeState(
    /** The newer release to announce; null hides the banner. */
    val release: AppRelease? = null,
)

sealed interface UpdateNoticeEvent {
    data object Open : UpdateNoticeEvent
    data object Dismiss : UpdateNoticeEvent
}

sealed interface UpdateNoticeEffect {
    data class OpenUrl(val url: String) : UpdateNoticeEffect
}
