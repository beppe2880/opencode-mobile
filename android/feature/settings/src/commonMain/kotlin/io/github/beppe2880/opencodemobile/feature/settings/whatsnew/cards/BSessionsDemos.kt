package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.ui.components.OcBadge
import io.github.beppe2880.opencodemobile.core.ui.components.OcButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcDivider
import io.github.beppe2880.opencodemobile.core.ui.components.OcProjectAvatar
import io.github.beppe2880.opencodemobile.core.ui.components.OcProviderInitial
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoFinger
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoLoop
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTargets
import kotlinx.coroutines.delay

private val Below = Offset(0f, 110f)

// ---- Browse server folders when starting a session ----

private val FOLDERS = mapOf(
    emptyList<String>() to listOf("projects", "notes"),
    listOf("projects") to listOf("api-server", "mobile-app"),
    listOf("projects", "mobile-app") to listOf("android", "iosApp"),
)

/** The path field of the New session sheet gets a Browse button: walk the server's folders and pick one. */
@Composable
internal fun FolderBrowseDemo(active: Boolean) {
    var browsing by remember { mutableStateOf(true) }
    var path by remember { mutableStateOf(listOf("projects", "mobile-app")) }
    var picked by remember { mutableStateOf("") }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(
        active,
        finalFrame = {
            browsing = true
            path = listOf("projects")
            picked = ""
            finger.hide()
        },
    ) {
        browsing = false
        path = emptyList()
        picked = ""
        finger.hide()
        delay(900)
        finger.show(targets.center("browse") + Below)
        finger.moveTo(targets.center("browse"))
        finger.tap()
        browsing = true
        delay(800)
        finger.moveTo(targets.center("folder0"))
        finger.tap()
        path = listOf("projects")
        delay(800)
        finger.moveTo(targets.center("folder1"))
        finger.tap()
        path = listOf("projects", "mobile-app")
        delay(800)
        finger.moveTo(targets.center("use"))
        finger.tap()
        picked = "/home/projects/mobile-app"
        browsing = false
        delay(600)
        finger.hide()
        delay(2200)
    }
    val c = OcTheme.colors
    Box(Modifier.fillMaxSize().background(c.bgBase).then(targets.root())) {
        AnimatedContent(browsing, transitionSpec = { fadeIn() togetherWith fadeOut() }) { isBrowsing ->
            if (isBrowsing) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = OcSpacing.md).heightIn(min = 36.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(OcSpacing.md),
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = c.iconBase, modifier = Modifier.size(20.dp))
                        Text("Choose folder", style = MaterialTheme.typography.titleSmall, color = c.textBase, modifier = Modifier.weight(1f))
                        Icon(Icons.Outlined.Visibility, contentDescription = null, tint = c.iconBase, modifier = Modifier.size(20.dp))
                    }
                    Row(
                        Modifier.padding(horizontal = OcSpacing.md).padding(bottom = OcSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs),
                    ) {
                        Icon(Icons.Outlined.Dns, contentDescription = null, tint = c.iconMuted, modifier = Modifier.size(14.dp))
                        val segments = listOf("home") + path
                        segments.forEachIndexed { index, name ->
                            Text("/", style = OcTheme.mono.codeSmall, color = c.textFaint)
                            val current = index == segments.lastIndex
                            Text(
                                name,
                                style = OcTheme.mono.codeSmall,
                                color = if (current) c.textBase else c.textMuted,
                                maxLines = 1,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(OcRadius.sm))
                                    .then(
                                        if (current) {
                                            Modifier.background(c.layer02).border(1.dp, c.borderStrong, RoundedCornerShape(OcRadius.sm))
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    OcDivider()
                    AnimatedContent(path, modifier = Modifier.weight(1f), transitionSpec = { fadeIn() togetherWith fadeOut() }) { current ->
                        Column {
                            (FOLDERS[current] ?: emptyList()).forEachIndexed { index, name ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 36.dp)
                                        .then(targets.target("folder$index"))
                                        .padding(horizontal = OcSpacing.lg),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(OcSpacing.md),
                                ) {
                                    Icon(Icons.Outlined.Folder, contentDescription = null, tint = c.iconBase, modifier = Modifier.size(18.dp))
                                    Text(name, style = MaterialTheme.typography.bodyMedium, color = c.textBase, modifier = Modifier.weight(1f))
                                    Icon(
                                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = c.iconMuted,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                                OcDivider()
                            }
                        }
                    }
                    OcButton(
                        "Use this folder",
                        onClick = {},
                        compact = true,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = OcSpacing.lg, vertical = OcSpacing.sm).then(targets.target("use")),
                    )
                }
            } else {
                Column(Modifier.fillMaxSize().padding(OcSpacing.lg), verticalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
                    Text("New session", style = MaterialTheme.typography.titleMedium, color = c.textBase)
                    Text("Absolute path", style = MaterialTheme.typography.labelMedium, color = c.textMuted)
                    val shape = RoundedCornerShape(OcRadius.md)
                    Row(
                        Modifier.fillMaxWidth().clip(shape).border(1.dp, c.borderFocus, shape).padding(start = OcSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            picked.ifEmpty { "/home/me/project" },
                            style = OcTheme.mono.code,
                            color = if (picked.isEmpty()) c.textFaint else c.textBase,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Row(
                            Modifier
                                .padding(vertical = OcSpacing.sm, horizontal = OcSpacing.sm)
                                .clip(RoundedCornerShape(OcRadius.sm))
                                .background(c.bgBase)
                                .border(1.dp, c.borderFocus, RoundedCornerShape(OcRadius.sm))
                                .padding(horizontal = OcSpacing.sm, vertical = OcSpacing.xs)
                                .then(targets.target("browse")),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs),
                        ) {
                            Icon(Icons.Outlined.FolderOpen, contentDescription = null, tint = c.textAccent, modifier = Modifier.size(16.dp))
                            Text("Browse", style = MaterialTheme.typography.labelMedium, color = c.textAccent)
                        }
                    }
                }
            }
        }
        finger.Draw()
    }
}

// ---- Connect providers with API keys and OAuth ----

private class DemoProvider(val name: String)

private val OPENAI = DemoProvider("OpenAI")
private val POPULAR = listOf(OPENAI, DemoProvider("Google"), DemoProvider("GitHub Copilot"))

@Composable
private fun ProviderRow(provider: DemoProvider, connected: Boolean, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    Row(
        modifier.fillMaxWidth().heightIn(min = 32.dp).padding(horizontal = OcSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.md),
    ) {
        OcProviderInitial(provider.name, size = 22.dp)
        Text(provider.name, style = MaterialTheme.typography.bodyMedium, color = c.textBase, modifier = Modifier.weight(1f))
        if (connected) OcBadge("Connected", c.success)
    }
}

@Composable
private fun GroupHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = OcTheme.colors.textMuted,
        modifier = Modifier.padding(horizontal = OcSpacing.lg, vertical = 2.dp),
    )
}

/** Pick a provider, paste its API key, connect: the credentials land on the server. */
@Composable
internal fun ConnectProviderDemo(active: Boolean) {
    var form by remember { mutableStateOf(false) }
    var keyLength by remember { mutableIntStateOf(0) }
    var saving by remember { mutableStateOf(false) }
    var connected by remember { mutableStateOf(true) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(
        active,
        finalFrame = {
            form = false
            keyLength = 0
            saving = false
            connected = true
            finger.hide()
        },
    ) {
        form = false
        keyLength = 0
        saving = false
        connected = false
        finger.hide()
        delay(1000)
        finger.show(targets.center("openai") + Below)
        finger.moveTo(targets.center("openai"))
        finger.tap()
        form = true
        delay(800)
        finger.moveTo(targets.center("key"))
        finger.tap()
        repeat(14) {
            keyLength++
            delay(70)
        }
        delay(300)
        finger.moveTo(targets.center("connect"))
        finger.tap()
        saving = true
        delay(900)
        saving = false
        connected = true
        form = false
        delay(500)
        finger.hide()
        delay(2400)
    }
    val c = OcTheme.colors
    Box(Modifier.fillMaxSize().background(c.bgBase).then(targets.root())) {
        AnimatedContent(form, transitionSpec = { fadeIn() togetherWith fadeOut() }) { isForm ->
            if (isForm) {
                Column(Modifier.fillMaxSize().padding(OcSpacing.lg), verticalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OcSpacing.md)) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = c.iconBase, modifier = Modifier.size(20.dp))
                        OcProviderInitial("OpenAI", size = 22.dp)
                        Text("OpenAI", style = MaterialTheme.typography.titleMedium, color = c.textBase)
                    }
                    Text("API key", style = MaterialTheme.typography.labelMedium, color = c.textMuted)
                    val shape = RoundedCornerShape(OcRadius.md)
                    Text(
                        if (keyLength == 0) "sk-…" else "•".repeat(keyLength),
                        style = OcTheme.mono.code,
                        color = if (keyLength == 0) c.textFaint else c.textBase,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .border(1.dp, c.borderFocus, shape)
                            .padding(horizontal = OcSpacing.md, vertical = OcSpacing.md)
                            .then(targets.target("key")),
                    )
                    Text("Credentials are saved on this server, not on your phone.", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                    OcButton(
                        "Connect",
                        onClick = {},
                        loading = saving,
                        compact = true,
                        modifier = Modifier.fillMaxWidth().then(targets.target("connect")),
                    )
                }
            } else {
                Column(Modifier.fillMaxSize().padding(vertical = OcSpacing.md), verticalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
                    Text(
                        "Connect provider",
                        style = MaterialTheme.typography.titleMedium,
                        color = c.textBase,
                        modifier = Modifier.padding(horizontal = OcSpacing.lg),
                    )
                    val shape = RoundedCornerShape(OcRadius.md)
                    Row(
                        Modifier
                            .padding(horizontal = OcSpacing.lg)
                            .fillMaxWidth()
                            .clip(shape)
                            .border(1.dp, c.borderBase, shape)
                            .padding(horizontal = OcSpacing.md, vertical = OcSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
                    ) {
                        Icon(Icons.Outlined.Search, contentDescription = null, tint = c.iconMuted, modifier = Modifier.size(16.dp))
                        Text("Search providers", style = MaterialTheme.typography.bodySmall, color = c.textFaint)
                    }
                    if (connected) {
                        GroupHeader("Connected")
                        ProviderRow(OPENAI, connected = true)
                    }
                    GroupHeader("Popular")
                    POPULAR.filter { !(connected && it === OPENAI) }.forEach { provider ->
                        ProviderRow(provider, connected = false, modifier = if (provider === OPENAI) targets.target("openai") else Modifier)
                    }
                }
            }
        }
        finger.Draw()
    }
}

// ---- Search projects in the New session sheet ----

private class DemoProject(val name: String, val path: String)

private val PROJECTS = listOf(
    DemoProject("api-server", "/home/me/api-server"),
    DemoProject("website", "/home/me/website"),
    DemoProject("docs", "/home/me/docs"),
    DemoProject("infra", "/home/me/infra"),
    DemoProject("notes", "/home/me/notes"),
    DemoProject("mobile-app", "/home/me/mobile-app"),
    DemoProject("mobile-kit", "/home/me/mobile-kit"),
)

private const val COLLAPSED = 3

/** Type in the project picker's search field: all projects are filtered, not just the first few. */
@Composable
internal fun ProjectSearchDemo(active: Boolean) {
    var query by remember { mutableStateOf("mob") }
    var focused by remember { mutableStateOf(true) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = { query = "mob"; focused = true; finger.hide() }) {
        query = ""
        focused = false
        finger.hide()
        delay(1000)
        finger.show(targets.center("search") + Below)
        finger.moveTo(targets.center("search"))
        finger.tap()
        focused = true
        delay(400)
        for (n in 1..3) {
            query = "mob".take(n)
            delay(350)
        }
        delay(2600)
        finger.hide()
        delay(300)
    }
    val c = OcTheme.colors
    Column(
        Modifier.fillMaxSize().then(targets.root()).padding(horizontal = OcSpacing.md, vertical = OcSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(OcSpacing.xs),
    ) {
        val shape = RoundedCornerShape(OcRadius.md)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .border(1.dp, if (focused) c.borderFocus else c.borderBase, shape)
                .padding(horizontal = OcSpacing.md, vertical = OcSpacing.sm)
                .then(targets.target("search")),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
        ) {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = c.iconMuted, modifier = Modifier.size(16.dp))
            Text(
                query.ifEmpty { "Search projects" },
                style = MaterialTheme.typography.bodyMedium,
                color = if (query.isEmpty()) c.textFaint else c.textBase,
            )
        }
        PROJECTS.forEachIndexed { index, project ->
            val visible = if (query.isEmpty()) index < COLLAPSED else project.name.contains(query, ignoreCase = true)
            AnimatedVisibility(visible, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 38.dp).padding(horizontal = OcSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(OcSpacing.md),
                ) {
                    OcProjectAvatar(project.name)
                    Column(Modifier.weight(1f)) {
                        Text(project.name, style = MaterialTheme.typography.bodyMedium, color = c.textBase, maxLines = 1)
                        Text(project.path, style = OcTheme.mono.codeSmall, color = c.textFaint, maxLines = 1)
                    }
                    val selected = index == 0
                    Box(Modifier.size(18.dp).clip(CircleShape).border(2.dp, if (selected) c.bgAccent else c.iconMuted, CircleShape).padding(4.dp)) {
                        if (selected) Box(Modifier.fillMaxSize().clip(CircleShape).background(c.bgAccent))
                    }
                }
            }
        }
        AnimatedVisibility(query.isEmpty(), enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Text(
                "Show all ${PROJECTS.size} projects",
                style = MaterialTheme.typography.labelLarge,
                color = c.textAccent,
                modifier = Modifier.padding(horizontal = OcSpacing.xs, vertical = OcSpacing.xs),
            )
        }
    }
    Box(Modifier.fillMaxSize()) { finger.Draw() }
}
