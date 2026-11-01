package io.github.beppe2880.opencodemobile.navigation

import androidx.compose.foundation.layout.Column
import io.github.beppe2880.opencodemobile.core.domain.SettingsArea
import io.github.beppe2880.opencodemobile.feature.chat.AppearancePreview
import io.github.beppe2880.opencodemobile.feature.chat.ChatPreview
import io.github.beppe2880.opencodemobile.feature.chat.ComposerPreview
import io.github.beppe2880.opencodemobile.feature.chat.LiveActivityPreview
import io.github.beppe2880.opencodemobile.feature.sessions.NewChatPreview
import io.github.beppe2880.opencodemobile.feature.sessions.SessionsPreview
import io.github.beppe2880.opencodemobile.feature.settings.customize.AreaPreviewContent
import io.github.beppe2880.opencodemobile.feature.terminal.TerminalPreview

/**
 * The previews Settings shows for the other screens. Settings cannot depend on them, so the
 * app, which knows every feature, hands them over; each feature draws its own real components.
 */
internal val settingsPreviews: AreaPreviewContent = { area, preview, modifier ->
    when (area) {
        SettingsArea.Appearance -> AppearancePreview(preview, modifier)
        SettingsArea.Chat -> ChatPreview(preview, modifier)
        SettingsArea.LiveActivity -> LiveActivityPreview(preview, modifier)
        SettingsArea.Composer -> Column(modifier) {
            ComposerPreview(preview)
            NewChatPreview(preview)
        }
        SettingsArea.Sessions -> SessionsPreview(preview, modifier)
        SettingsArea.Terminal -> TerminalPreview(preview, modifier)
        // Drawn by Settings itself.
        SettingsArea.Notifications, SettingsArea.Updates -> Unit
    }
}
