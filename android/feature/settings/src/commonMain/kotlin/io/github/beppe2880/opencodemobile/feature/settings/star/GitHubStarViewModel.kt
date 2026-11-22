package io.github.beppe2880.opencodemobile.feature.settings.star

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import io.github.beppe2880.opencodemobile.core.domain.APP_REPOSITORY_URL
import io.github.beppe2880.opencodemobile.core.domain.GitHubStarRepository
import io.github.beppe2880.opencodemobile.core.domain.GitHubStarState
import io.github.beppe2880.opencodemobile.core.mvi.BaseViewModel
import kotlinx.coroutines.launch

@Immutable
data class GitHubStarUiState(
    /** "Star on GitHub" was tapped in this run of the app: the invitation thanks until a restart. */
    val thanked: Boolean = false,
    /** The banner above the sessions, shown once after a while of use. */
    val bannerVisible: Boolean = false,
)

sealed interface GitHubStarEvent {
    /** "Star on GitHub" or the banner was tapped: opens the repository. */
    data object Star : GitHubStarEvent

    /** The banner's X: it never comes back. */
    data object DismissBanner : GitHubStarEvent
}

sealed interface GitHubStarEffect {
    data class OpenUrl(val url: String) : GitHubStarEffect
}

/**
 * The invitation to star the project on GitHub, for the banner above the sessions and the card of
 * Tips & new features. With [countUse] (the banner) opening it counts a day of use, which shows
 * the banner once it is due.
 */
class GitHubStarViewModel(
    private val repository: GitHubStarRepository,
    countUse: Boolean = false,
) : BaseViewModel<GitHubStarUiState, GitHubStarEvent, GitHubStarEffect>(repository.state.value.toUi()) {

    init {
        viewModelScope.launch { repository.state.collect { star -> updateState { star.toUi() } } }
        if (countUse) viewModelScope.launch { repository.recordUse() }
    }

    override fun onEvent(event: GitHubStarEvent) {
        when (event) {
            GitHubStarEvent.Star -> {
                sendEffect(GitHubStarEffect.OpenUrl(APP_REPOSITORY_URL))
                viewModelScope.launch { repository.starred() }
            }
            GitHubStarEvent.DismissBanner -> viewModelScope.launch { repository.dismissBanner() }
        }
    }
}

internal fun GitHubStarState.toUi() =
    GitHubStarUiState(thanked = thanked, bannerVisible = bannerVisible)
