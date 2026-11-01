package io.github.beppe2880.opencodemobile.feature.settings.customize

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.AppRelease
import io.github.beppe2880.opencodemobile.core.domain.SettingsArea
import io.github.beppe2880.opencodemobile.core.domain.SettingsPart
import io.github.beppe2880.opencodemobile.core.ui.components.OcBadge
import io.github.beppe2880.opencodemobile.core.ui.components.OcCustomizable
import io.github.beppe2880.opencodemobile.core.ui.components.OcLogo
import io.github.beppe2880.opencodemobile.core.ui.components.PartPreview
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_preview_allow
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_preview_app
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_preview_finished_text
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_preview_finished_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_preview_not_sent
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_preview_permission_text
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_preview_permission_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_preview_question_text
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_preview_question_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_preview_reject
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_preview_update_off
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_preview_update_on
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_preview_watching
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_preview_watching_text
import io.github.beppe2880.opencodemobile.feature.settings.update.UpdateNotice
import io.github.beppe2880.opencodemobile.feature.settings.update.UpdateNoticeState
import io.github.beppe2880.opencodemobile.feature.settings.update.UpdateTarget
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The preview of a group of settings as a column of parts: the groups about other screens come
 * from [preview], supplied by the app, which knows those screens; notifications and the update
 * banner belong to Settings.
 */
typealias AreaPreviewContent = @Composable (area: SettingsArea, preview: PartPreview, modifier: Modifier) -> Unit

@Composable
internal fun AreaPreview(
    area: SettingsArea,
    preview: PartPreview,
    content: AreaPreviewContent,
    modifier: Modifier = Modifier,
    updateTarget: UpdateTarget = UpdateTarget.Android,
) {
    when (area) {
        SettingsArea.Notifications -> NotificationsPreview(preview, modifier)
        SettingsArea.Updates -> UpdatesPreview(preview, updateTarget, modifier)
        else -> content(area, preview, modifier)
    }
}

/**
 * The notifications a session sends, drawn like the system shows them: what the watching
 * service shows while sessions run, then one per kind. Those that would not be sent are faded.
 */
@Composable
private fun NotificationsPreview(preview: PartPreview, modifier: Modifier) {
    val s = preview.settings
    Column(modifier.fillMaxWidth().padding(OcSpacing.md), verticalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
        OcCustomizable(SettingsPart.NotificationShade, preview) {
            NotificationMock(Res.string.settings_preview_watching, Res.string.settings_preview_watching_text, sent = s.notifications)
        }
        OcCustomizable(SettingsPart.PermissionNotification, preview) {
            NotificationMock(
                Res.string.settings_preview_permission_title,
                Res.string.settings_preview_permission_text,
                sent = s.notifications && s.notifyPermissions,
                actions = listOf(Res.string.settings_preview_allow, Res.string.settings_preview_reject),
            )
        }
        OcCustomizable(SettingsPart.QuestionNotification, preview) {
            NotificationMock(Res.string.settings_preview_question_title, Res.string.settings_preview_question_text, sent = s.notifications && s.notifyQuestions)
        }
        OcCustomizable(SettingsPart.FinishedNotification, preview) {
            NotificationMock(Res.string.settings_preview_finished_title, Res.string.settings_preview_finished_text, sent = s.notifications && s.notifyFinished)
        }
    }
}

@Composable
private fun NotificationMock(title: StringResource, text: StringResource, sent: Boolean, actions: List<StringResource> = emptyList()) {
    val c = OcTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.layer01, RoundedCornerShape(OcRadius.xl))
            .padding(OcSpacing.md),
        verticalArrangement = Arrangement.spacedBy(OcSpacing.xxs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
            OcLogo(size = 16.dp, modifier = Modifier.alpha(if (sent) 1f else 0.4f))
            Text(
                stringResource(Res.string.settings_preview_app),
                style = MaterialTheme.typography.labelSmall,
                color = c.textMuted,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (!sent) OcBadge(stringResource(Res.string.settings_preview_not_sent), c.warning)
        }
        Column(Modifier.alpha(if (sent) 1f else 0.4f)) {
            Text(
                stringResource(title),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = c.textBase,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(stringResource(text), style = MaterialTheme.typography.bodySmall, color = c.textMuted)
            if (actions.isNotEmpty()) {
                Row(Modifier.padding(top = OcSpacing.xs), horizontalArrangement = Arrangement.spacedBy(OcSpacing.lg)) {
                    actions.forEach { Text(stringResource(it), style = MaterialTheme.typography.labelLarge, color = c.textAccent) }
                }
            }
        }
    }
}

/** The real update banner with a sample release, and where it shows; faded when the app never looks. */
@Composable
private fun UpdatesPreview(preview: PartPreview, target: UpdateTarget, modifier: Modifier) {
    val on = preview.settings.checkForUpdates
    val release = AppRelease("1.4.0", "https://github.com/beppe2880/opencode-mobile/releases")
    Column(modifier.fillMaxWidth().padding(OcSpacing.md)) {
        OcCustomizable(SettingsPart.UpdateBanner, preview) {
            Column(verticalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
                UpdateNotice(
                    state = UpdateNoticeState(release, target.actionFor(release)),
                    onEvent = {},
                    modifier = Modifier.alpha(if (on) 1f else 0.4f),
                )
                Text(
                    stringResource(if (on) Res.string.settings_preview_update_on else Res.string.settings_preview_update_off),
                    style = MaterialTheme.typography.bodySmall,
                    color = OcTheme.colors.textMuted,
                )
            }
        }
    }
}
