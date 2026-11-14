package io.github.beppe2880.opencodemobile.feature.settings.whatsnew

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.AppRelease
import io.github.beppe2880.opencodemobile.core.domain.AppVersion
import io.github.beppe2880.opencodemobile.core.ui.components.OcBadge
import io.github.beppe2880.opencodemobile.core.ui.components.OcButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcButtonVariant
import io.github.beppe2880.opencodemobile.core.ui.components.OcCard
import io.github.beppe2880.opencodemobile.core.ui.components.OcDivider
import io.github.beppe2880.opencodemobile.core.ui.components.OcEmptyState
import io.github.beppe2880.opencodemobile.core.ui.components.OcErrorState
import io.github.beppe2880.opencodemobile.core.ui.components.OcIconButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcSkeleton
import io.github.beppe2880.opencodemobile.core.ui.components.OcSpinner
import io.github.beppe2880.opencodemobile.core.ui.components.OcTopBar
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_back
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_retry
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcStateColors
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.feature_section_changelog
import io.github.beppe2880.opencodemobile.feature.settings.resources.update_check_failed
import io.github.beppe2880.opencodemobile.feature.settings.resources.update_download
import io.github.beppe2880.opencodemobile.feature.settings.resources.update_install
import io.github.beppe2880.opencodemobile.feature.settings.resources.update_release_notes
import io.github.beppe2880.opencodemobile.feature.settings.resources.update_sheet_version
import io.github.beppe2880.opencodemobile.feature.settings.resources.whats_new_also_includes
import io.github.beppe2880.opencodemobile.feature.settings.resources.whats_new_empty
import io.github.beppe2880.opencodemobile.feature.settings.resources.whats_new_empty_message
import io.github.beppe2880.opencodemobile.feature.settings.resources.whats_new_error
import io.github.beppe2880.opencodemobile.feature.settings.resources.whats_new_installed
import io.github.beppe2880.opencodemobile.feature.settings.resources.whats_new_installed_version
import io.github.beppe2880.opencodemobile.feature.settings.resources.whats_new_no_notes
import io.github.beppe2880.opencodemobile.feature.settings.resources.whats_new_offline
import io.github.beppe2880.opencodemobile.feature.settings.resources.whats_new_open_change
import io.github.beppe2880.opencodemobile.feature.settings.resources.whats_new_refresh
import io.github.beppe2880.opencodemobile.feature.settings.resources.whats_new_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.whats_new_type_fixed
import io.github.beppe2880.opencodemobile.feature.settings.resources.whats_new_type_new
import io.github.beppe2880.opencodemobile.feature.settings.resources.whats_new_type_other
import io.github.beppe2880.opencodemobile.feature.settings.update.UpdateAction
import io.github.beppe2880.opencodemobile.feature.settings.update.UpdateActionKind
import io.github.beppe2880.opencodemobile.feature.settings.update.UpdateTarget
import io.github.beppe2880.opencodemobile.feature.settings.usage.formatMediumDate
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import io.github.beppe2880.opencodemobile.core.ui.resources.Res as UiRes

/** Room for the timeline's line and dots beside the days. */
private val RailWidth = 28.dp

/** The line runs through the middle of the dots. */
private val RailCenter = 8.dp

/** Height of a day's first line, which the dot is centred on. */
private val DayHeaderHeight = 28.dp

/**
 * The release history as a timeline of days, newest first: each day with its date, the last
 * version published that day and its changes as New, Fixed and Other.
 */
@Composable
fun WhatsNewScreen(state: WhatsNewState, onEvent: (WhatsNewEvent) -> Unit, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    Scaffold(
        modifier = modifier.testTag("whats_new_page"),
        containerColor = c.bgDeep,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            OcTopBar(
                title = stringResource(Res.string.feature_section_changelog),
                leading = {
                    OcIconButton(
                        icon = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(UiRes.string.oc_back),
                        onClick = { onEvent(WhatsNewEvent.Back) },
                        modifier = Modifier.testTag("whats_new_back_button"),
                    )
                },
                actions = {
                    when {
                        // Without days the placeholders already say it is loading.
                        state.loading && state.days.isNotEmpty() -> OcSpinner(Modifier.padding(end = OcSpacing.lg))
                        !state.loading -> OcIconButton(
                            icon = Icons.Outlined.Refresh,
                            contentDescription = stringResource(Res.string.whats_new_refresh),
                            onClick = { onEvent(WhatsNewEvent.Refresh) },
                            modifier = Modifier.testTag("whats_new_refresh_button"),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                // Wide windows keep the lines at a readable length, in the middle.
                modifier = Modifier.widthIn(max = 640.dp).fillMaxSize().testTag("whats_new_list"),
                contentPadding = PaddingValues(
                    start = OcSpacing.gutter,
                    end = OcSpacing.gutter,
                    top = OcSpacing.lg,
                    bottom = OcSpacing.xxl + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                ),
            ) {
                when {
                    state.days.isNotEmpty() -> {
                        if (state.failed) item(key = "offline", contentType = "notice") { OfflineNotice(onRetry = { onEvent(WhatsNewEvent.Refresh) }) }
                        itemsIndexed(state.days, key = { _, day -> day.date.toString() }, contentType = { _, _ -> "day" }) { index, day ->
                            Day(
                                day = day,
                                first = index == 0,
                                last = index == state.days.lastIndex,
                                installedVersion = state.installedVersion,
                                update = state.update,
                                onEvent = onEvent,
                            )
                        }
                    }
                    state.loading -> items(3, key = { "skeleton$it" }, contentType = { "skeleton" }) { DaySkeleton() }
                    state.failed -> item(key = "error") {
                        OcErrorState(
                            title = stringResource(Res.string.whats_new_error),
                            message = stringResource(Res.string.update_check_failed),
                            onRetry = { onEvent(WhatsNewEvent.Refresh) },
                            modifier = Modifier.testTag("whats_new_error"),
                            retryTestTag = "whats_new_retry_button",
                        )
                    }
                    else -> item(key = "empty") {
                        OcEmptyState(
                            icon = Icons.Outlined.NewReleases,
                            title = stringResource(Res.string.whats_new_empty),
                            message = stringResource(Res.string.whats_new_empty_message),
                            modifier = Modifier.testTag("whats_new_empty"),
                        )
                    }
                }
            }
        }
    }
}

/**
 * One day of the timeline: a dot on the line, the date with the version published that day, its
 * changes in a card and, when the version is newer than the installed one, the update action.
 */
@Composable
private fun Day(
    day: WhatsNewDay,
    first: Boolean,
    last: Boolean,
    installedVersion: String,
    update: UpdateAction?,
    onEvent: (WhatsNewEvent) -> Unit,
) {
    val c = OcTheme.colors
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val line = c.borderStrong
    val halo = c.bgDeep
    val dot = when {
        day.updateAvailable -> c.bgAccent
        day.installed -> c.success.fg
        else -> c.layer04
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val x = RailCenter.toPx().let { if (rtl) size.width - it else it }
                val y = DayHeaderHeight.toPx() / 2
                // The line joins the dots: it starts at the first one and ends at the last.
                drawLine(line, Offset(x, if (first) y else 0f), Offset(x, if (last) y else size.height), strokeWidth = 1.dp.toPx())
                drawCircle(halo, radius = 7.dp.toPx(), center = Offset(x, y))
                drawCircle(dot, radius = 4.dp.toPx(), center = Offset(x, y))
            }
            .padding(start = RailWidth, bottom = OcSpacing.xl)
            .testTag("whats_new_day_${day.date}"),
        verticalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        DayHeader(day, installedVersion)
        if (day.alsoIncludes.isNotEmpty()) {
            Text(
                stringResource(Res.string.whats_new_also_includes, day.alsoIncludes.joinToString(", ")),
                style = MaterialTheme.typography.bodySmall,
                color = c.textMuted,
                modifier = Modifier.testTag("whats_new_also_${day.date}"),
            )
        }
        OcCard(Modifier.fillMaxWidth()) {
            if (day.groups.isEmpty()) {
                Text(
                    stringResource(Res.string.whats_new_no_notes),
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textMuted,
                    modifier = Modifier.padding(OcSpacing.lg),
                )
            }
            day.groups.forEachIndexed { index, group ->
                if (index > 0) OcDivider()
                ChangeGroup(group, tagPrefix = "whats_new_change_${day.date}", onEvent = onEvent)
            }
        }
        if (day.updateAvailable && update != null) {
            OcButton(
                text = stringResource(
                    when (update.kind) {
                        UpdateActionKind.Download -> Res.string.update_download
                        UpdateActionKind.ReleaseNotes -> Res.string.update_release_notes
                        UpdateActionKind.Store -> Res.string.update_install
                    },
                ),
                onClick = { onEvent(WhatsNewEvent.Update) },
                // One call to action: the newest day stands out, the days between follow quietly.
                variant = if (first) OcButtonVariant.Primary else OcButtonVariant.Secondary,
                leadingIcon = if (update.kind == UpdateActionKind.ReleaseNotes) null else Icons.Outlined.Download,
                compact = true,
                modifier = Modifier.testTag("whats_new_update_${day.date}"),
            )
        }
    }
}

/** The date, then the version of the day as a pill and the mark of the installed version. */
@Composable
private fun DayHeader(day: WhatsNewDay, installedVersion: String) {
    val c = OcTheme.colors
    val date = remember(day.date) { formatMediumDate(day.date) }
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = DayHeaderHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        Text(
            date,
            style = MaterialTheme.typography.titleSmall,
            color = c.textBase,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        if (day.installed) {
            // Another version of the same day may be the installed one.
            val same = AppVersion.parse(day.version) == AppVersion.parse(installedVersion)
            val installed = if (same) {
                stringResource(Res.string.whats_new_installed)
            } else {
                stringResource(Res.string.whats_new_installed_version, installedVersion)
            }
            OcBadge(
                text = installed,
                colors = c.success,
                modifier = Modifier.testTag("whats_new_installed"),
            )
        }
        VersionPill(day.version, highlighted = day.updateAvailable)
    }
}

@Composable
private fun VersionPill(version: String, highlighted: Boolean) {
    val c = OcTheme.colors
    val colors = if (highlighted) c.info else OcStateColors(fg = c.textMuted, bg = c.layer02, border = c.borderBase)
    val description = stringResource(Res.string.update_sheet_version, version)
    Text(
        "v$version",
        // Versions read left to right in every language.
        style = OcTheme.mono.label.copy(textDirection = TextDirection.Ltr),
        color = colors.fg,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(colors.bg)
            .border(1.dp, colors.border, RoundedCornerShape(percent = 50))
            .padding(horizontal = OcSpacing.sm, vertical = OcSpacing.xxs)
            .clearAndSetSemantics { contentDescription = description },
    )
}

/** The changes of one type under its coloured label. */
@Composable
private fun ChangeGroup(group: WhatsNewGroup, tagPrefix: String, onEvent: (WhatsNewEvent) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = OcSpacing.sm)) {
        TypeLabel(group.type, Modifier.padding(start = OcSpacing.lg, end = OcSpacing.lg, top = OcSpacing.xs, bottom = OcSpacing.xs))
        group.changes.forEachIndexed { index, change ->
            ChangeRow(change, tag = "${tagPrefix}_${group.type.name.lowercase()}_$index", onEvent = onEvent)
        }
    }
}

@Composable
private fun TypeLabel(type: ChangeType, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    val colors = when (type) {
        ChangeType.New -> c.success
        ChangeType.Fixed -> c.info
        ChangeType.Other -> OcStateColors(fg = c.textMuted, bg = c.layer02, border = c.borderBase)
    }
    val icon: ImageVector = when (type) {
        ChangeType.New -> Icons.Outlined.AutoAwesome
        ChangeType.Fixed -> Icons.Outlined.BugReport
        ChangeType.Other -> Icons.Outlined.Tune
    }
    val label = stringResource(
        when (type) {
            ChangeType.New -> Res.string.whats_new_type_new
            ChangeType.Fixed -> Res.string.whats_new_type_fixed
            ChangeType.Other -> Res.string.whats_new_type_other
        },
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(OcRadius.sm))
            .background(colors.bg)
            .border(1.dp, colors.border, RoundedCornerShape(OcRadius.sm))
            .padding(horizontal = OcSpacing.sm, vertical = 3.dp)
            .semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs),
    ) {
        Icon(icon, contentDescription = null, tint = colors.fg, modifier = Modifier.size(12.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.fg)
    }
}

/** A change with its scope as a small label; with a link, tapping it opens the issue or pull request. */
@Composable
private fun ChangeRow(change: WhatsNewChange, tag: String, onEvent: (WhatsNewEvent) -> Unit) {
    val c = OcTheme.colors
    val url = change.url
    val open = stringResource(Res.string.whats_new_open_change)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (url != null) {
                    Modifier.clickable(onClickLabel = open, role = Role.Button) { onEvent(WhatsNewEvent.OpenChange(url)) }.heightIn(min = 48.dp)
                } else {
                    Modifier.heightIn(min = 36.dp)
                },
            )
            .padding(horizontal = OcSpacing.lg, vertical = OcSpacing.xs)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        Text(change.text, style = MaterialTheme.typography.bodyMedium, color = c.textBase, modifier = Modifier.weight(1f))
        change.scope?.let { ScopeLabel(it) }
        if (url != null) {
            Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, tint = c.iconMuted, modifier = Modifier.size(14.dp))
        }
    }
}

/** The part of the app a change concerns (`chat`, `sessions`), as written in the release notes. */
@Composable
private fun ScopeLabel(scope: String) {
    val c = OcTheme.colors
    Text(
        scope,
        style = OcTheme.mono.label,
        color = c.textMuted,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(OcRadius.xs))
            .background(c.layer02)
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

/** Above the kept releases when GitHub could not be reached. */
@Composable
private fun OfflineNotice(onRetry: () -> Unit) {
    val c = OcTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = OcSpacing.lg)
            .clip(RoundedCornerShape(OcRadius.lg))
            .background(c.layer01)
            .padding(start = OcSpacing.md, end = OcSpacing.xs, top = OcSpacing.xs, bottom = OcSpacing.xs)
            .testTag("whats_new_offline"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = c.iconMuted, modifier = Modifier.size(18.dp))
        Text(
            stringResource(Res.string.whats_new_offline),
            style = MaterialTheme.typography.bodySmall,
            color = c.textMuted,
            modifier = Modifier.weight(1f).padding(vertical = OcSpacing.xs).semantics { liveRegion = LiveRegionMode.Polite },
        )
        OcButton(
            text = stringResource(UiRes.string.oc_retry),
            onClick = onRetry,
            variant = OcButtonVariant.Ghost,
            compact = true,
            modifier = Modifier.testTag("whats_new_retry_button"),
        )
    }
}

@Composable
private fun DaySkeleton() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = RailWidth, bottom = OcSpacing.xl).clearAndSetSemantics { },
        verticalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        Box(Modifier.height(DayHeaderHeight), contentAlignment = Alignment.CenterStart) {
            OcSkeleton(Modifier.width(120.dp).height(16.dp))
        }
        OcSkeleton(Modifier.fillMaxWidth().height(132.dp), RoundedCornerShape(OcRadius.lg))
    }
}

@PreviewLightDark
@Composable
private fun WhatsNewScreenPreview() {
    OcTheme(darkTheme = androidx.compose.foundation.isSystemInDarkTheme()) {
        val issue = "https://github.com/beppe2880/opencode-mobile/issues/1"
        val release = AppRelease("0.10.0", "https://github.com/beppe2880/opencode-mobile/releases/tag/v0.10.0")
        WhatsNewScreen(
            state = WhatsNewState(
                loading = false,
                installedVersion = "0.9.0",
                update = UpdateTarget.Android.actionFor(release),
                days = listOf(
                    WhatsNewDay(
                        date = LocalDate(2026, 11, 10),
                        version = "0.10.0",
                        groups = listOf(
                            WhatsNewGroup(
                                ChangeType.New,
                                listOf(
                                    WhatsNewChange("See what changed in every release", "settings", issue),
                                    WhatsNewChange("Pick a project from the list header", "sessions", issue),
                                ),
                            ),
                            WhatsNewGroup(ChangeType.Fixed, listOf(WhatsNewChange("Keep the model chip in sight while typing", "chat", issue))),
                        ),
                        updateAvailable = true,
                    ),
                    WhatsNewDay(
                        date = LocalDate(2026, 10, 27),
                        version = "0.9.0",
                        groups = listOf(
                            WhatsNewGroup(ChangeType.New, listOf(WhatsNewChange("Follow the agent's work live", "chat", issue))),
                            WhatsNewGroup(ChangeType.Fixed, listOf(WhatsNewChange("Restore touch scrolling in the terminal", "terminal", issue))),
                            WhatsNewGroup(ChangeType.Other, listOf(WhatsNewChange("Faster start"))),
                        ),
                        installed = true,
                    ),
                ),
            ),
            onEvent = {},
        )
    }
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = OcTheme.colors.textBase,
        modifier = modifier.padding(bottom = OcSpacing.md).semantics { heading() },
    )
}
