package io.github.beppe2880.opencodemobile.navigation

import androidx.compose.foundation.layout.Column
import io.github.beppe2880.opencodemobile.core.domain.SettingsArea
import io.github.beppe2880.opencodemobile.feature.chat.AppearancePreview
import io.github.beppe2880.opencodemobile.feature.chat.ChatPreview
import io.github.beppe2880.opencodemobile.feature.chat.ComposerPreview
import io.github.beppe2880.opencodemobile.feature.chat.LiveActivityPreview
import io.github.beppe2880.opencodemobile.feature.chat.VoiceInputPreview
import io.github.beppe2880.opencodemobile.feature.sessions.NewChatPreview
import io.github.beppe2880.opencodemobile.feature.sessions.SessionsPreview
import io.github.beppe2880.opencodemobile.feature.sessions.SharingPreview
import io.github.beppe2880.opencodemobile.feature.sessions.SplitViewPreview
import io.github.beppe2880.opencodemobile.feature.sessions.SwipeChatsPreview
import io.github.beppe2880.opencodemobile.feature.settings.customize.AreaPreviewContent
import io.github.beppe2880.opencodemobile.feature.terminal.TerminalPreview

/**
 * The previews Settings shows for the other screens. Settings cannot depend on them, so the
 * app, which knows every feature, hands them over; each feature draws its own real components.
 */
internal val settingsPreviews: AreaPreviewContent = { area, preview, modifier ->
    when (area) {
        SettingsArea.Appearance -> Column(modifier) {
            AppearancePreview(preview)
            SplitViewPreview(preview)
        }
        SettingsArea.Chat -> Column(modifier) {
            ChatPreview(preview)
            SwipeChatsPreview(preview)
        }
        SettingsArea.LiveActivity -> LiveActivityPreview(preview, modifier)
        SettingsArea.Composer -> Column(modifier) {
            ComposerPreview(preview)
            NewChatPreview(preview)
        }
        SettingsArea.VoiceInput -> VoiceInputPreview(preview, modifier)
        SettingsArea.Sessions -> SessionsPreview(preview, modifier)
        SettingsArea.Sharing -> SharingPreview(preview, modifier)
        SettingsArea.Terminal -> TerminalPreview(preview, modifier)
        // Drawn by Settings itself, or without a preview.
        SettingsArea.Notifications, SettingsArea.Updates, SettingsArea.Servers, SettingsArea.About -> Unit
    }
}
