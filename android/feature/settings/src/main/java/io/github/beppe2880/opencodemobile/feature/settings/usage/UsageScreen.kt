package io.github.beppe2880.opencodemobile.feature.settings.usage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.DailyActivity
import io.github.beppe2880.opencodemobile.core.domain.ModelRef
import io.github.beppe2880.opencodemobile.core.domain.ModelUsage
import io.github.beppe2880.opencodemobile.core.domain.TokenUsage
import io.github.beppe2880.opencodemobile.core.domain.UsageStats
import io.github.beppe2880.opencodemobile.core.ui.R as UiR
import io.github.beppe2880.opencodemobile.core.ui.components.OcCard
import io.github.beppe2880.opencodemobile.core.ui.components.OcDivider
import io.github.beppe2880.opencodemobile.core.ui.components.OcEmptyState
import io.github.beppe2880.opencodemobile.core.ui.components.OcErrorState
import io.github.beppe2880.opencodemobile.core.ui.components.OcIconButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcSegment
import io.github.beppe2880.opencodemobile.core.ui.components.OcSegmentedControl
import io.github.beppe2880.opencodemobile.core.ui.components.OcSkeleton
import io.github.beppe2880.opencodemobile.core.ui.components.OcSpinner
import io.github.beppe2880.opencodemobile.core.ui.components.OcTopBar
import io.github.beppe2880.opencodemobile.core.ui.text.formatCost
import io.github.beppe2880.opencodemobile.core.ui.text.formatTokens
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.R
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun UsageScreen(state: UsageState, onEvent: (UsageEvent) -> Unit, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    Scaffold(
        modifier = modifier,
        containerColor = c.bgDeep,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            OcTopBar(
                title = stringResource(R.string.usage_title),
                leading = {
                    OcIconButton(
                        icon = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(UiR.string.oc_back),
                        onClick = { onEvent(UsageEvent.Back) },
                        modifier = Modifier.testTag("usage_back_button"),
                    )
                },
                actions = { if (state.loading && state.stats != null) OcSpinner(Modifier.padding(end = OcSpacing.lg)) },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxSize().testTag("usage_list"),
                contentPadding = PaddingValues(
                    start = OcSpacing.gutter,
                    end = OcSpacing.gutter,
                    top = OcSpacing.lg,
                    bottom = OcSpacing.xxl + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                ),
                verticalArrangement = Arrangement.spacedBy(OcSpacing.md),
            ) {
                item(key = "period") {
                    OcSegmentedControl(
                        segments = listOf(
                            OcSegment(UsagePeriod.Today, stringResource(R.string.usage_today), "usage_period_today"),
                            OcSegment(UsagePeriod.Week, stringResource(R.string.usage_week), "usage_period_week"),
                            OcSegment(UsagePeriod.Month, stringResource(R.string.usage_month), "usage_period_month"),
                        ),
                        selected = state.period,
                        onSelect = { onEvent(UsageEvent.SelectPeriod(it)) },
                    )
                }
                val stats = state.stats
                when {
                    stats == null && state.error != null -> item(key = "error") {
                        OcErrorState(
                            title = stringResource(R.string.usage_error),
                            message = state.error.resolve(),
                            onRetry = { onEvent(UsageEvent.Retry) },
                            retryTestTag = "usage_retry_button",
                        )
                    }
                    stats == null -> items(3, key = { "skeleton$it" }) {
                        OcSkeleton(Modifier.fillMaxWidth().height(96.dp), RoundedCornerShape(OcRadius.lg))
                    }
                    stats.steps == 0L -> item(key = "empty") {
                        OcEmptyState(
                            icon = Icons.Outlined.QueryStats,
                            title = stringResource(R.string.usage_empty),
                            message = stringResource(R.string.usage_empty_message),
                            modifier = Modifier.testTag("usage_empty"),
                        )
                    }
                    else -> {
                        item(key = "summary") { Summary(stats) }
                        if (state.period != UsagePeriod.Today) item(key = "activity") { Activity(stats.activity, state.period) }
                        item(key = "models") { Models(stats, state.modelNames) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Summary(stats: UsageStats) {
    val c = OcTheme.colors
    val numbers = NumberFormat.getIntegerInstance()
    OcCard(Modifier.fillMaxWidth().testTag("usage_summary")) {
        Column(Modifier.padding(OcSpacing.lg), verticalArrangement = Arrangement.spacedBy(OcSpacing.md)) {
            Column {
                Text(stringResource(R.string.usage_cost), style = MaterialTheme.typography.labelMedium, color = c.textMuted)
                Text(
                    formatCost(stats.cost).ifEmpty { "$0" },
                    style = MaterialTheme.typography.headlineMedium,
                    color = c.textBase,
                    modifier = Modifier.testTag("usage_cost"),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(OcSpacing.md)) {
                Figure(stringResource(R.string.usage_prompts), numbers.format(stats.prompts), Modifier.weight(1f))
                Figure(stringResource(R.string.usage_sessions), numbers.format(stats.sessions), Modifier.weight(1f))
                Figure(stringResource(R.string.usage_steps), numbers.format(stats.steps), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(OcSpacing.md)) {
                Figure(stringResource(R.string.usage_input), formatTokens(stats.tokens.input + stats.tokens.cacheRead + stats.tokens.cacheWrite), Modifier.weight(1f))
                Figure(stringResource(R.string.usage_output), formatTokens(stats.tokens.output + stats.tokens.reasoning), Modifier.weight(1f))
                Figure(
                    stringResource(R.string.usage_cached),
                    cachedShare(stats.tokens)?.let { "$it%" } ?: "–",
                    Modifier.weight(1f),
                )
            }
            if (stats.activeDays > 1) {
                Text(
                    pluralStringResource(R.plurals.usage_active_days, stats.activeDays.toInt(), stats.activeDays.toInt()) +
                        if (stats.streak > 1) " · " + pluralStringResource(R.plurals.usage_streak, stats.streak.toInt(), stats.streak.toInt()) else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textMuted,
                )
            }
        }
    }
}

/** Share of the input read from the provider cache, which is what keeps long sessions cheap. */
private fun cachedShare(tokens: TokenUsage): Int? {
    val input = tokens.input + tokens.cacheRead + tokens.cacheWrite
    return if (input == 0L) null else (tokens.cacheRead * 100 / input).toInt()
}

@Composable
private fun Figure(label: String, value: String, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    Column(modifier.semantics(mergeDescendants = true) {}) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, style = OcTheme.mono.code, color = c.textBase, maxLines = 1)
    }
}

/** One bar per day; the chart is announced as a sentence instead of bar by bar. */
@Composable
private fun Activity(days: List<DailyActivity>, period: UsagePeriod) {
    val c = OcTheme.colors
    val max = days.maxOfOrNull { it.steps }?.takeIf { it > 0 } ?: return
    val busiest = days.maxBy { it.steps }
    val description = stringResource(R.string.usage_activity_description, dayLabel(busiest.date, long = true), busiest.steps)
    OcCard(Modifier.fillMaxWidth().testTag("usage_activity")) {
        Column(Modifier.padding(OcSpacing.lg), verticalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
            Text(stringResource(R.string.usage_activity), style = MaterialTheme.typography.labelMedium, color = c.textMuted)
            Row(
                modifier = Modifier.fillMaxWidth().height(96.dp).semantics { contentDescription = description },
                horizontalArrangement = Arrangement.spacedBy(if (period == UsagePeriod.Week) OcSpacing.sm else 2.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                days.forEach { day ->
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight((day.steps.toFloat() / max).coerceAtLeast(if (day.steps > 0) 0.03f else 0f))
                                .background(c.bgAccent, RoundedCornerShape(topStart = OcRadius.xs, topEnd = OcRadius.xs)),
                        )
                    }
                }
            }
            if (period == UsagePeriod.Week) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
                    days.forEach { day ->
                        Text(
                            dayLabel(day.date, long = false),
                            style = MaterialTheme.typography.labelSmall,
                            color = c.textFaint,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            } else if (days.size > 1) {
                Row(Modifier.fillMaxWidth()) {
                    Text(dayLabel(days.first().date, long = true), style = MaterialTheme.typography.labelSmall, color = c.textFaint, modifier = Modifier.weight(1f))
                    Text(dayLabel(days.last().date, long = true), style = MaterialTheme.typography.labelSmall, color = c.textFaint)
                }
            }
        }
    }
}

/** `Mon` (short) or `Oct 12` (long) in the device language; the raw date when it does not parse. */
private fun dayLabel(date: String, long: Boolean): String = runCatching {
    val day = LocalDate.parse(date)
    if (long) day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()))
    else day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
}.getOrDefault(date)

@Composable
private fun Models(stats: UsageStats, names: Map<String, ModelName>) {
    val c = OcTheme.colors
    val numbers = NumberFormat.getIntegerInstance()
    // Subscription models report no cost: they are compared by steps instead.
    val byCost = stats.cost > 0
    val total = if (byCost) stats.cost else stats.steps.toDouble()
    OcCard(Modifier.fillMaxWidth().testTag("usage_models")) {
        Text(
            stringResource(R.string.usage_models),
            style = MaterialTheme.typography.labelMedium,
            color = c.textMuted,
            modifier = Modifier.padding(start = OcSpacing.lg, end = OcSpacing.lg, top = OcSpacing.lg),
        )
        stats.models.forEachIndexed { index, usage ->
            if (index > 0) OcDivider()
            val name = names[usage.model.key] ?: ModelName(usage.model.id, usage.model.providerId)
            val share = if (total > 0) ((if (byCost) usage.cost else usage.steps.toDouble()) / total).toFloat() else 0f
            Column(
                Modifier.fillMaxWidth().padding(horizontal = OcSpacing.lg, vertical = OcSpacing.sm).testTag("usage_model_${usage.model.key}"),
                verticalArrangement = Arrangement.spacedBy(OcSpacing.xs),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
                    Column(Modifier.weight(1f)) {
                        Text(name.name, style = MaterialTheme.typography.bodyMedium, color = c.textBase, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            name.provider + " · " + pluralStringResource(R.plurals.usage_model_steps, usage.steps.toInt(), numbers.format(usage.steps)),
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(formatCost(usage.cost).ifEmpty { "–" }, style = OcTheme.mono.code, color = c.textBase)
                }
                LinearProgressIndicator(
                    progress = { share.coerceIn(0f, 1f) },
                    color = c.bgAccent,
                    trackColor = c.layer03,
                    drawStopIndicator = {},
                    gapSize = 0.dp,
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                )
            }
        }
        Box(Modifier.height(OcSpacing.sm))
    }
}

@PreviewLightDark
@Composable
private fun UsageScreenPreview() {
    OcTheme(darkTheme = androidx.compose.foundation.isSystemInDarkTheme()) {
        UsageScreen(
            state = UsageState(
                loading = false,
                stats = UsageStats(
                    sessions = 44, subagents = 19, prompts = 681, steps = 6697, cost = 617.94,
                    tokens = TokenUsage(input = 1_340_828, output = 4_330_765, reasoning = 382_391, cacheRead = 1_731_115_320, cacheWrite = 37_389_482),
                    activeDays = 7, streak = 7,
                    activity = listOf(1614L, 1336, 974, 1595, 284, 631, 263).mapIndexed { i, s -> DailyActivity("2026-10-1${2 + i}", s) },
                    models = listOf(
                        ModelUsage(ModelRef("claude-opus-5-5", "anthropic"), 6502, 617.37, TokenUsage()),
                        ModelUsage(ModelRef("deepseek-v4.1-flash", "opencode-go"), 81, 0.21, TokenUsage()),
                    ),
                ),
            ),
            onEvent = {},
        )
    }
}
