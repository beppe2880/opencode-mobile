package io.github.beppe2880.opencodemobile.feature.settings.whatsnew

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.AppVersion
import io.github.beppe2880.opencodemobile.core.ui.components.OcBadge
import io.github.beppe2880.opencodemobile.core.ui.components.OcButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcButtonVariant
import io.github.beppe2880.opencodemobile.core.ui.components.OcCard
import io.github.beppe2880.opencodemobile.core.ui.components.OcIconButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcTopBar
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_back
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.AppInfo
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.feature_new
import io.github.beppe2880.opencodemobile.feature.settings.resources.feature_open_pr
import io.github.beppe2880.opencodemobile.feature.settings.resources.feature_section_changelog
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_feature_count
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_installed
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_newer
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_show_carousel
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_show_list
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_version
import io.github.beppe2880.opencodemobile.feature.settings.resources.whats_new_title
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import io.github.beppe2880.opencodemobile.core.ui.resources.Res as UiRes

/** How the features of each version are shown; chosen with the button of the top bar. */
internal enum class TipsLayout { Carousel, List }

// Kept for the whole run of the app; a setting once the layout is chosen for good.
private var tipsLayout by mutableStateOf(TipsLayout.Carousel)

/** The new features of every version, newest first, each opening its own page. */
@Composable
fun TipsRoute(
    onBack: () -> Unit,
    onOpenChangelog: () -> Unit,
    onOpenVersion: (String) -> Unit,
    onOpenFeature: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = OcTheme.colors
    val installed = koinInject<AppInfo>().versionName
    val versions = featureCards.groupBy { it.version }.toList().sortedByDescending { (version, _) -> version.versionKey() }
    Scaffold(
        modifier = modifier.testTag("tips_page"),
        containerColor = c.bgDeep,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            OcTopBar(
                title = stringResource(Res.string.whats_new_title),
                leading = {
                    OcIconButton(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(UiRes.string.oc_back), onClick = onBack)
                },
                actions = {
                    val carousel = tipsLayout == TipsLayout.Carousel
                    OcIconButton(
                        icon = if (carousel) Icons.AutoMirrored.Outlined.ViewList else Icons.Outlined.ViewCarousel,
                        contentDescription = stringResource(if (carousel) Res.string.tips_show_list else Res.string.tips_show_carousel),
                        onClick = { tipsLayout = if (carousel) TipsLayout.List else TipsLayout.Carousel },
                        modifier = Modifier.testTag("tips_layout_button"),
                    )
                    OcIconButton(
                        icon = Icons.Outlined.History,
                        contentDescription = stringResource(Res.string.feature_section_changelog),
                        onClick = onOpenChangelog,
                        modifier = Modifier.testTag("tips_changelog_button"),
                    )
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxSize(),
                contentPadding = PaddingValues(
                    start = OcSpacing.gutter,
                    end = OcSpacing.gutter,
                    top = OcSpacing.lg,
                    bottom = OcSpacing.xxl + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                ),
                verticalArrangement = Arrangement.spacedBy(OcSpacing.xl),
            ) {
                items(versions, key = { it.first }) { (version, cards) ->
                    VersionSection(version, cards, versionStatus(version, installed), tipsLayout, onOpenVersion = { onOpenVersion(version) }, onOpenFeature)
                }
            }
        }
    }
}

@Composable
private fun VersionSection(
    version: String,
    cards: List<FeatureCard>,
    status: VersionStatus,
    layout: TipsLayout,
    onOpenVersion: () -> Unit,
    onOpenFeature: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(OcSpacing.md)) {
        VersionHeader(version, cards.size, status, onClick = onOpenVersion)
        when (layout) {
            TipsLayout.Carousel -> {
                val pager = rememberPagerState { cards.size }
                HorizontalPager(
                    state = pager,
                    contentPadding = PaddingValues(end = if (cards.size > 1) 40.dp else 0.dp),
                    pageSpacing = OcSpacing.sm,
                    verticalAlignment = Alignment.Top,
                ) { page ->
                    FeatureTile(cards[page], active = pager.settledPage == page, onClick = { onOpenFeature(cards[page].id) })
                }
                if (cards.size > 1) PagerDots(cards.size, pager.currentPage)
            }
            TipsLayout.List -> cards.forEach { card ->
                FeatureTile(card, active = true, onClick = { onOpenFeature(card.id) })
            }
        }
    }
}

@Composable
private fun PagerDots(count: Int, current: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
        repeat(count) { index ->
            Box(
                Modifier
                    .size(width = if (index == current) 18.dp else 6.dp, height = 6.dp)
                    .background(if (index == current) OcTheme.colors.textBase else OcTheme.colors.borderStrong, CircleShape),
            )
        }
    }
}

/** A feature in the list of its version: its demo, title and first lines; tapping opens its page. */
@Composable
private fun FeatureTile(card: FeatureCard, active: Boolean, onClick: () -> Unit) {
    val c = OcTheme.colors
    OcCard(modifier = Modifier.fillMaxWidth().testTag("feature_tile_${card.id}"), onClick = onClick) {
        DemoStage(card, active, height = 200.dp)
        Column(
            Modifier.padding(start = OcSpacing.lg, end = OcSpacing.lg, top = OcSpacing.xs, bottom = OcSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(OcSpacing.xs),
        ) {
            Text(stringResource(card.title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = c.textBase)
            Text(
                stringResource(card.body),
                style = MaterialTheme.typography.bodyMedium,
                color = c.textMuted,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The page of one feature: its demo in full, what it does, who made it and its pull request. */
@Composable
fun FeatureRoute(id: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    val card = featureCards.firstOrNull { it.id == id } ?: return
    val uriHandler = LocalUriHandler.current
    val openUrl: (String) -> Unit = { runCatching { uriHandler.openUri(it) } }
    Scaffold(
        modifier = modifier.testTag("feature_page_${card.id}"),
        containerColor = c.bgDeep,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            OcTopBar(
                title = stringResource(card.title),
                leading = { OcIconButton(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(UiRes.string.oc_back), onClick = onBack) },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = OcSpacing.gutter, vertical = OcSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(OcSpacing.md),
            ) {
                OcCard(Modifier.fillMaxWidth()) { DemoStage(card, active = true, height = 320.dp) }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
                    OcBadge(stringResource(Res.string.feature_new), c.success)
                    Text(stringResource(Res.string.tips_version, card.version), style = MaterialTheme.typography.labelMedium, color = c.textFaint)
                }
                Text(stringResource(card.title), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold), color = c.textBase)
                Text(stringResource(card.body), style = MaterialTheme.typography.bodyLarge, color = c.textMuted)
                Author(card, openUrl)
                card.pullRequest?.let { pr ->
                    OcButton(
                        text = stringResource(Res.string.feature_open_pr),
                        onClick = { openUrl("https://github.com/beppe2880/opencode-mobile/pull/$pr") },
                        variant = OcButtonVariant.Secondary,
                    )
                }
            }
        }
    }
}

/** Sorts "0.16.0" after "0.9.0". */
private fun String.versionKey(): String = split('.').joinToString(".") { it.padStart(4, '0') }

/** Where a version stands against the installed app: its title and badge show it. */
internal enum class VersionStatus { Installed, Newer, Older }

internal fun versionStatus(version: String, installed: String): VersionStatus {
    val v = AppVersion.parse(version) ?: return VersionStatus.Older
    val i = AppVersion.parse(installed) ?: return VersionStatus.Older
    return when {
        v == i -> VersionStatus.Installed
        v > i -> VersionStatus.Newer
        else -> VersionStatus.Older
    }
}

/** The title of a version: colored and badged by [status]; tapping it opens the version's page. */
@Composable
private fun VersionHeader(version: String, count: Int, status: VersionStatus, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    val color = when (status) {
        VersionStatus.Installed -> c.success.fg
        VersionStatus.Newer -> c.textAccent
        VersionStatus.Older -> c.textBase
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OcRadius.md))
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(vertical = OcSpacing.xs)
            .testTag("tips_version_$version"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Text(
            stringResource(Res.string.tips_version, version),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = color,
            modifier = Modifier.semantics { heading() },
        )
        when (status) {
            VersionStatus.Installed -> OcBadge(stringResource(Res.string.tips_installed), c.success)
            VersionStatus.Newer -> OcBadge(stringResource(Res.string.tips_newer), c.info)
            VersionStatus.Older -> Unit
        }
        Spacer(Modifier.weight(1f))
        Text(pluralStringResource(Res.plurals.tips_feature_count, count, count), style = MaterialTheme.typography.labelMedium, color = c.textFaint)
        if (onClick != null) Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = c.iconMuted)
    }
}

/** Every new feature of one version, as a list; each opens its own page. */
@Composable
fun VersionRoute(version: String, onBack: () -> Unit, onOpenFeature: (String) -> Unit, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    val installed = koinInject<AppInfo>().versionName
    val cards = featureCards.filter { it.version == version }
    Scaffold(
        modifier = modifier.testTag("tips_version_page_$version"),
        containerColor = c.bgDeep,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            OcTopBar(
                title = stringResource(Res.string.tips_version, version),
                leading = { OcIconButton(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(UiRes.string.oc_back), onClick = onBack) },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxSize(),
                contentPadding = PaddingValues(
                    start = OcSpacing.gutter,
                    end = OcSpacing.gutter,
                    top = OcSpacing.lg,
                    bottom = OcSpacing.xxl + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                ),
                verticalArrangement = Arrangement.spacedBy(OcSpacing.md),
            ) {
                item(key = "header") { VersionHeader(version, cards.size, versionStatus(version, installed), onClick = null) }
                items(cards, key = { it.id }) { card -> FeatureTile(card, active = true, onClick = { onOpenFeature(card.id) }) }
            }
        }
    }
}
