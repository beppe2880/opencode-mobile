package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.ui.markdown.OcMarkdown
import io.github.beppe2880.opencodemobile.core.ui.markdown.rememberMarkdownCache
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.core.ui.theme.OpenCodeThemes
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoComposer
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoFinger
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoLoop
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTargets
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTopBar
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoUserBubble
import kotlinx.coroutines.delay

private class ThemeChoice(val id: String, val dark: Boolean)

private val THEME_CHOICES = listOf(
    ThemeChoice("github", dark = false),
    ThemeChoice("dracula", dark = true),
    ThemeChoice("gruvbox", dark = true),
    ThemeChoice("nord", dark = true),
)

private val FINAL_THEME = 1

/** A small chat redrawn with each theme: pick one in the strip and the whole scene takes its colors. */
@Composable
internal fun OpenCodeThemesDemo(active: Boolean) {
    var index by remember { mutableIntStateOf(FINAL_THEME) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = { index = FINAL_THEME; finger.hide() }) {
        index = 0
        finger.hide()
        delay(1100)
        finger.show(targets.center("theme1") + Offset(0f, 90f))
        for (next in 1..3) {
            finger.moveTo(targets.center("theme$next"))
            finger.tap()
            index = next
            delay(1200)
        }
        finger.hide()
        delay(700)
    }
    val c = OcTheme.colors
    Column(Modifier.fillMaxSize().then(targets.root())) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Crossfade(index, animationSpec = tween(450)) { current ->
                val choice = THEME_CHOICES[current]
                OcTheme(darkTheme = choice.dark, openCodeTheme = choice.id) { ThemedChat() }
            }
        }
        Row(
            Modifier.fillMaxWidth().background(c.bgBase).padding(horizontal = OcSpacing.md, vertical = OcSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OcSpacing.md),
        ) {
            THEME_CHOICES.forEachIndexed { i, choice ->
                val preview = remember(choice.id) { OpenCodeThemes.byId(choice.id)?.preview(choice.dark) }
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(preview?.background ?: c.layer02)
                        .border(if (i == index) 2.dp else 1.dp, if (i == index) c.borderFocus else c.borderStrong, CircleShape)
                        .then(targets.target("theme$i")),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(preview?.primary ?: c.bgAccent))
                }
            }
            Text(
                OpenCodeThemes.byId(THEME_CHOICES[index].id)?.displayName.orEmpty(),
                style = MaterialTheme.typography.labelLarge,
                color = c.textBase,
                modifier = Modifier.weight(1f),
            )
        }
    }
    Box(Modifier.fillMaxSize()) { finger.Draw() }
}

@Composable
private fun ThemedChat() {
    val c = OcTheme.colors
    val cache = rememberMarkdownCache()
    BoxWithConstraints(Modifier.fillMaxSize().background(c.bgBase)) {
        val tall = maxHeight > 220.dp
        Column(Modifier.fillMaxSize()) {
            DemoTopBar("Fix login redirect", subtitle = "api-server")
            Column(Modifier.weight(1f).padding(horizontal = OcSpacing.md), verticalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
                DemoUserBubble("Fix the login redirect")
                BToolRow(Icons.Outlined.Edit, "Edit", "src/auth.ts", Modifier.fillMaxWidth())
                OcMarkdown(id = "theme-demo", content = "Done: it keeps the **original path** and uses `redirectTo`.", cache = cache)
            }
            if (tall) DemoComposer(Modifier.padding(horizontal = OcSpacing.sm).padding(bottom = OcSpacing.sm))
        }
    }
}
