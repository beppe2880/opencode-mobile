package io.github.beppe2880.opencodemobile.navigation

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
 * Only when the layout has just become [split] ([widened]) or is still [waiting]: in a wide
 * window a chat pushed above the list on purpose (a subagent opened from the chat beside the list)
 * stays full screen, so Back returns to its parent. A terminal, file or settings screen above
 * that chat is kept; the chat moves beside the list once they close, and a return to the list
 * itself stops the wait.
 */
internal fun splitStep(split: Boolean, widened: Boolean, waiting: Boolean, top: Any?, below: Any?): SplitStep {
    if (!split || (!widened && !waiting)) return SplitStep()
    val chat = (top as? ChatDestination)?.takeIf { below == SessionsDestination }
    return when {
        chat != null -> SplitStep(selectChat = chat.sessionId)
        top == null || top == SessionsDestination || top is ConnectDestination -> SplitStep()
        else -> SplitStep(waiting = true)
    }
}
