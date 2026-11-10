package io.github.beppe2880.opencodemobile.feature.settings.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.beppe2880.opencodemobile.core.mvi.CollectEffect
import io.github.beppe2880.opencodemobile.core.ui.components.OcButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcButtonVariant
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.update_check
import io.github.beppe2880.opencodemobile.feature.settings.resources.update_check_checking
import io.github.beppe2880.opencodemobile.feature.settings.resources.update_check_failed
import io.github.beppe2880.opencodemobile.feature.settings.resources.update_check_latest
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * "Check for updates" under the update notice choice in Settings; null where it cannot work, such
 * as previews and the live demo's options, which then show the choice alone.
 */
internal val LocalUpdateCheck = staticCompositionLocalOf<(@Composable () -> Unit)?> { null }

/** The ViewModel of the updates in Settings: it never looks or opens the sheet on its own. */
@Composable
private fun settingsUpdates(): UpdateNoticeViewModel = koinViewModel { parametersOf(false) }

/**
 * Hosts Settings' update sheet, opened by "Check for updates", and provides that button to the
 * pages under [content]. Outside the pages, so leaving the Updates page keeps the sheet.
 */
@Composable
fun UpdateCheckHost(content: @Composable () -> Unit) {
    val viewModel = settingsUpdates()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is UpdateNoticeEffect.OpenUrl -> runCatching { uriHandler.openUri(effect.url) }
        }
    }
    CompositionLocalProvider(LocalUpdateCheck provides UpdateCheckSlot) {
        content()
    }
    if (state.sheetVisible) UpdateSheet(state, viewModel::onEvent)
}

/** One instance, so providing it never recomposes the pages. */
private val UpdateCheckSlot: @Composable () -> Unit = { UpdateCheckRoute() }

@Composable
private fun UpdateCheckRoute() {
    val viewModel = settingsUpdates()
    val state by viewModel.state.collectAsStateWithLifecycle()
    UpdateCheckRow(state.check, state.installedVersion, onCheck = { viewModel.onEvent(UpdateNoticeEvent.CheckNow) })
}

/** The button, a spinner in it while GitHub is asked, then the result under it. */
@Composable
internal fun UpdateCheckRow(check: UpdateCheck, installedVersion: String, onCheck: () -> Unit, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    Column(
        modifier = modifier.fillMaxWidth().padding(start = OcSpacing.lg, end = OcSpacing.lg, bottom = OcSpacing.md),
        verticalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        OcButton(
            text = stringResource(Res.string.update_check),
            onClick = onCheck,
            variant = OcButtonVariant.Secondary,
            loading = check == UpdateCheck.Checking,
            modifier = Modifier.fillMaxWidth().testTag("settings_update_check_button"),
        )
        val result = when (check) {
            UpdateCheck.Idle -> null
            UpdateCheck.Checking -> stringResource(Res.string.update_check_checking)
            UpdateCheck.UpToDate -> stringResource(Res.string.update_check_latest, installedVersion)
            UpdateCheck.Failed -> stringResource(Res.string.update_check_failed)
        }
        if (result != null) {
            Text(
                result,
                style = MaterialTheme.typography.bodySmall,
                color = if (check == UpdateCheck.Failed) c.danger.fg else c.textMuted,
                // Read out when it changes, since the focus stays on the button.
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag("settings_update_check_result"),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun UpdateCheckRowPreview() {
    OcTheme(darkTheme = androidx.compose.foundation.isSystemInDarkTheme()) {
        UpdateCheckRow(UpdateCheck.UpToDate, "1.3.0", onCheck = {})
    }
}
