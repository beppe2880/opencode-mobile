package io.github.beppe2880.opencodemobile.feature.settings.update

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.AppRelease
import io.github.beppe2880.opencodemobile.core.domain.ReleaseNote
import io.github.beppe2880.opencodemobile.core.domain.ReleaseSection
import io.github.beppe2880.opencodemobile.core.domain.ReleaseSectionKind
import io.github.beppe2880.opencodemobile.core.domain.UpdateNoticeStyle
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_preview_update_feature
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_preview_update_fix
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_preview_update_off
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_preview_update_on
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_preview_update_window
import org.jetbrains.compose.resources.stringResource

/**
 * The update notice with a sample release, as [style] shows it, and a line saying where: the
 * window's content, the banner, or the banner faded when the app never looks. Drawn by the
 * Settings preview and by the live demo, which never looks for updates; its buttons do nothing.
 * [compact] shows the banner for every style, to keep the screen around it in sight.
 */
@Composable
fun UpdateNoticeSample(style: UpdateNoticeStyle, target: UpdateTarget, modifier: Modifier = Modifier, compact: Boolean = false) {
    val release = AppRelease("1.4.0", "https://github.com/beppe2880/opencode-mobile/releases")
    val state = UpdateNoticeState(
        release = release,
        action = target.actionFor(release),
        style = style,
        installedVersion = "1.3.0",
        changes = listOf(
            VersionChanges(
                release.version,
                listOf(
                    ReleaseSection(
                        ReleaseSectionKind.Features,
                        "",
                        listOf(ReleaseNote(stringResource(Res.string.settings_preview_update_feature), "chat")),
                    ),
                    ReleaseSection(
                        ReleaseSectionKind.Fixes,
                        "",
                        listOf(ReleaseNote(stringResource(Res.string.settings_preview_update_fix), "chat")),
                    ),
                ),
            ),
        ),
    )
    Column(modifier.fillMaxWidth().testTag("update_notice_sample"), verticalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
        if (style == UpdateNoticeStyle.Window && !compact) {
            Surface(
                shape = RoundedCornerShape(OcRadius.sheet),
                color = OcTheme.colors.bgBase,
                border = BorderStroke(1.dp, OcTheme.colors.borderBase),
                modifier = Modifier.fillMaxWidth(),
            ) {
                UpdateSheetContent(state, onEvent = {}, modifier = Modifier.padding(top = OcSpacing.lg), scrollNotes = false)
            }
        } else {
            UpdateNotice(state, onEvent = {}, modifier = Modifier.alpha(if (style == UpdateNoticeStyle.Off) 0.4f else 1f))
        }
        Text(
            stringResource(
                when (style) {
                    UpdateNoticeStyle.Window -> Res.string.settings_preview_update_window
                    UpdateNoticeStyle.Banner -> Res.string.settings_preview_update_on
                    UpdateNoticeStyle.Off -> Res.string.settings_preview_update_off
                },
            ),
            style = MaterialTheme.typography.bodySmall,
            color = OcTheme.colors.textMuted,
        )
    }
}
