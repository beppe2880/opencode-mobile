package io.github.beppe2880.opencodemobile.feature.settings.whatsnew

import androidx.compose.runtime.Immutable
import io.github.beppe2880.opencodemobile.feature.settings.update.UpdateAction

@Immutable
data class WhatsNewState(
    /** The release days, newest first; from the releases kept on the device until GitHub answers. */
    val days: List<WhatsNewDay> = emptyList(),
    /** GitHub is being asked. With [days] they stay in place; without, placeholders stand in. */
    val loading: Boolean = true,
    /** The last request failed: a full error without [days], a notice above them otherwise. */
    val failed: Boolean = false,
    /** The version of this app. */
    val installedVersion: String = "",
    /**
     * What the Update button of the newer days does: the same action the update sheet offers for
     * the newest release. Null when the installed version is the newest.
     */
    val update: UpdateAction? = null,
)

sealed interface WhatsNewEvent {
    data object Back : WhatsNewEvent

    /** Ask GitHub again: the retry of the error, or the refresh button. */
    data object Refresh : WhatsNewEvent

    /** A change with a link was tapped: its issue or pull request on GitHub. */
    data class OpenChange(val url: String) : WhatsNewEvent

    /** The Update button of a day newer than the installed version. */
    data object Update : WhatsNewEvent
}

sealed interface WhatsNewEffect {
    data object NavigateBack : WhatsNewEffect
    data class OpenUrl(val url: String) : WhatsNewEffect
}

/** The "What's New" button above the session list. */
@Immutable
data class WhatsNewButtonState(
    /** Shown: not hidden, or a release was published since it was. */
    val visible: Boolean = false,
    /** There are releases the page was not opened for yet: the button shows a dot. */
    val unseen: Boolean = false,
)

sealed interface WhatsNewButtonEvent {
    data object Open : WhatsNewButtonEvent

    /** Hide the button until the next release. */
    data object Hide : WhatsNewButtonEvent
}

sealed interface WhatsNewButtonEffect {
    data object OpenPage : WhatsNewButtonEffect
}
