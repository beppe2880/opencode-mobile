package io.github.beppe2880.opencodemobile.navigation

import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.FoldChat
import io.github.beppe2880.opencodemobile.core.domain.UnfoldChat
import io.github.beppe2880.opencodemobile.core.domain.listBesideChat

/**
 * What the back stack does as the split layout comes and goes: [selectChat] is a chat opened
 * full screen right above the session list, to pop and show beside the list instead; [waiting]
 * keeps looking for one as screens above it close.
 */
internal data class SplitStep(val selectChat: String? = null, val waiting: Boolean = false)

/**
 * Decides, from the routes of the [top] entry and the one [below] it, whether a chat opened full
 * screen while the window was narrow (a folded foldable) becomes the detail of the split layout.
 *
 * Only when the layout has just become [split] ([widened]) or is still [waiting], and [unfold]
 * moves it beside the list: in a wide window a chat pushed above the list on purpose (a subagent
 * opened from the chat beside the list) stays full screen, so Back returns to its parent. A
 * terminal, file or settings screen above that chat is kept; the chat moves beside the list once
 * they close, and a return to the list itself stops the wait.
 */
internal fun splitStep(
    split: Boolean,
    widened: Boolean,
    waiting: Boolean,
    top: Any?,
    below: Any?,
    unfold: UnfoldChat = UnfoldChat.BesideList,
): SplitStep {
    if (!split || (!widened && !waiting) || unfold == UnfoldChat.FullScreen) return SplitStep()
    val chat = (top as? ChatDestination)?.takeIf { below == SessionsDestination }
    return when {
        chat != null -> SplitStep(selectChat = chat.sessionId)
        top == null || top == SessionsDestination || top is ConnectDestination -> SplitStep()
        else -> SplitStep(waiting = true)
    }
}

/** What the chat beside the list does when the split layout goes away. */
internal enum class FoldStep {
    /** Nothing: there was no split, no chat, or the layout was changed in Settings. */
    None,

    /** It opens full screen above the list, so Back returns to the list. */
    OpenFullScreen,

    /** It closes: the list shows, and the chat pane is empty when the window widens again. */
    CloseChat,
}

/**
 * Decides what the chat beside the list does when the split layout goes from [wasSplit] to
 * [split]. Only a window that [narrowed][wasSplit] while [settings] still put the list beside the
 * chat counts as folding; turning that off in Settings leaves the chat closed as before.
 */
internal fun foldStep(wasSplit: Boolean, split: Boolean, chatOpen: Boolean, settings: AppSettings): FoldStep = when {
    !wasSplit || split || !chatOpen || !settings.listBesideChat -> FoldStep.None
    settings.foldOpenChat == FoldChat.ShowList -> FoldStep.CloseChat
    else -> FoldStep.OpenFullScreen
}

/** Whether a window this [wide] shows the session list beside the chat. */
internal fun AppSettings.showsSplit(wide: Boolean): Boolean = wide && listBesideChat

/**
 * Whether the chat's swipe hides and shows the session list whatever the chat swipe choice: on
 * wide windows with [AppSettings.listSwipe], the list beside the chat or its panel over it.
 */
internal fun AppSettings.swipeShowsList(wide: Boolean): Boolean = wide && splitView && listSwipe
