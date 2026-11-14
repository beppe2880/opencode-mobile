package io.github.beppe2880.opencodemobile.feature.settings.whatsnew

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.ui.components.OcBadge
import io.github.beppe2880.opencodemobile.core.ui.components.OcCard
import io.github.beppe2880.opencodemobile.core.ui.markdown.OcMarkdown
import io.github.beppe2880.opencodemobile.core.ui.markdown.rememberMarkdownCache
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.feature_attachments_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.feature_attachments_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.feature_attachments_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.feature_by
import io.github.beppe2880.opencodemobile.feature.settings.resources.feature_math_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.feature_math_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.feature_math_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.feature_new
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards.tipsCardsA
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards.tipsCardsB
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards.tipsCardsC
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards.tipsCardsD
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards.tipsCardsE
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards.tipsCardsF
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * A new feature or a tip, shown with a live demo: the app's real components played by a script in
 * a loop, so it follows the theme, the text size and the language of the app.
 */
@Immutable
internal class FeatureCard(
    val id: String,
    val version: String,
    val icon: ImageVector,
    val title: StringResource,
    val body: StringResource,
    /** What the demo shows, for screen readers: the demo itself is not read. */
    val demoDescription: StringResource,
    val author: String,
    /** Null for what came with the first release, before pull requests. */
    val pullRequest: Int?,
    /** The demo; it plays only while [active], otherwise it shows its last frame. */
    val demo: @Composable (active: Boolean) -> Unit,
)

private const val REPOSITORY = "https://github.com/beppe2880/opencode-mobile"

/** Every card, newest version first within the page (the page groups and sorts them by version). */
internal val featureCards: List<FeatureCard> by lazy { baseCards + tipsCardsA + tipsCardsB + tipsCardsC + tipsCardsD + tipsCardsE + tipsCardsF }

private val baseCards = listOf(
    FeatureCard(
        id = "math-formulas",
        version = "0.16.0",
        icon = Icons.Outlined.Functions,
        title = Res.string.feature_math_title,
        body = Res.string.feature_math_body,
        demoDescription = Res.string.feature_math_demo,
        author = "beppe2880",
        pullRequest = 216,
        demo = { MathFormulasDemo(it) },
    ),
    FeatureCard(
        id = "attachment-thumbnails",
        version = "0.16.0",
        icon = Icons.Outlined.Image,
        title = Res.string.feature_attachments_title,
        body = Res.string.feature_attachments_body,
        demoDescription = Res.string.feature_attachments_demo,
        author = "beppe2880",
        pullRequest = 218,
        demo = { AttachmentsDemo(it) },
    ),
)

@Composable
internal fun DemoStage(card: FeatureCard, active: Boolean, height: Dp = DEMO_HEIGHT) {
    val c = OcTheme.colors
    val description = stringResource(card.demoDescription)
    Box(
        Modifier
            .padding(OcSpacing.sm)
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(OcRadius.md))
            .background(c.layer01)
            .border(1.dp, c.borderMuted, RoundedCornerShape(OcRadius.md))
            .clearAndSetSemantics { contentDescription = description },
    ) {
        card.demo(active)
    }
}

@Composable
internal fun Author(card: FeatureCard, onOpenUrl: (String) -> Unit, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(OcRadius.sm))
            .clickable(role = Role.Button, enabled = card.pullRequest != null) { card.pullRequest?.let { onOpenUrl("$REPOSITORY/pull/$it") } }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        Box(Modifier.size(24.dp).clip(CircleShape).background(c.bgAccent), contentAlignment = Alignment.Center) {
            Text(card.author.take(1).uppercase(), style = MaterialTheme.typography.labelMedium, color = c.onAccent)
        }
        Text(
            if (card.pullRequest != null) stringResource(Res.string.feature_by, "@${card.author}", card.pullRequest) else "@${card.author}",
            style = MaterialTheme.typography.labelLarge,
            color = c.textAccent,
        )
    }
}

private val DEMO_HEIGHT = 240.dp

// ---- Math formulas ----

private const val MATH_DEMO_REPLY =
    "Newton's second law: \$\\vec{F} = m\\vec{a}\$, and the kinetic energy is \$K = \\frac{1}{2}mv^2\$.\n\n" +
        "The speed of light follows from two constants:\n\n" +
        "\$\$\nc = \\frac{1}{\\sqrt{\\mu_0 \\varepsilon_0}}\n\$\$\n\n" +
        "And the Gaussian integral:\n\n" +
        "\$\$\n\\int_{-\\infty}^{\\infty} e^{-x^2}\\,dx = \\sqrt{\\pi}\n\$\$"

/** A reply streams in: each formula is plain text until it is closed, then it is typeset. */
@Composable
private fun MathFormulasDemo(active: Boolean) {
    val cache = rememberMarkdownCache()
    var shown by remember { mutableIntStateOf(MATH_DEMO_REPLY.length) }
    val fade = remember { Animatable(1f) }
    LaunchedEffect(active) {
        if (!active) {
            shown = MATH_DEMO_REPLY.length
            fade.snapTo(1f)
            return@LaunchedEffect
        }
        while (true) {
            shown = 0
            fade.snapTo(1f)
            delay(600)
            while (shown < MATH_DEMO_REPLY.length) {
                shown = minOf(MATH_DEMO_REPLY.length, shown + 3)
                delay(28)
            }
            delay(3500)
            fade.animateTo(0f, tween(400))
        }
    }
    Column(Modifier.fillMaxWidth().padding(OcSpacing.md).alpha(fade.value)) {
        OcMarkdown(id = "demo-math", content = MATH_DEMO_REPLY.take(shown), cache = cache)
    }
}

// ---- Attachment thumbnails ----

private val PICTURES = listOf(
    Brush.verticalGradient(listOf(Color(0xFF7FB3E8), Color(0xFFF6D7A7), Color(0xFF6E9B5C))),
    Brush.linearGradient(listOf(Color(0xFFF08A5D), Color(0xFFB83B5E), Color(0xFF6A2C70))),
)

/** Two pictures land in the message box, one is opened full screen, then removed with its X. */
@Composable
private fun AttachmentsDemo(active: Boolean) {
    val c = OcTheme.colors
    var count by remember { mutableIntStateOf(2) }
    var removed by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf(false) }
    val finger = remember { DemoFinger() }
    val targets = remember { mutableStateMapOf<String, Rect>() }
    var origin by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(active) {
        if (!active) {
            count = 2
            removed = false
            preview = false
            finger.hide()
            return@LaunchedEffect
        }
        while (true) {
            count = 0
            removed = false
            preview = false
            finger.hide()
            delay(700)
            count = 1
            delay(350)
            count = 2
            delay(700)
            finger.show(Offset(targets["thumb0"]?.center?.x ?: 0f, (targets["thumb0"]?.center?.y ?: 0f) + 140f))
            finger.moveTo(targets["thumb0"]?.center ?: Offset.Zero)
            finger.tap()
            preview = true
            delay(1600)
            finger.moveTo(targets["close"]?.center ?: Offset.Zero)
            finger.tap()
            preview = false
            delay(500)
            finger.moveTo(targets["remove1"]?.center ?: Offset.Zero)
            finger.tap()
            removed = true
            delay(400)
            finger.hide()
            delay(1800)
        }
    }
    Box(
        Modifier.fillMaxSize().onGloballyPositioned { origin = it.boundsInRoot().topLeft },
    ) {
        fun Modifier.target(name: String) = onGloballyPositioned { coordinates: LayoutCoordinates ->
            targets[name] = coordinates.boundsInRoot().translate(-origin)
        }
        // The message box, as in the chat.
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(OcSpacing.md)
                .fillMaxWidth()
                .clip(RoundedCornerShape(OcRadius.lg))
                .background(c.bgBase)
                .border(1.dp, c.borderFocus, RoundedCornerShape(OcRadius.lg))
                .padding(OcSpacing.md),
        ) {
            Row(Modifier.height(64.dp), horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
                PICTURES.forEachIndexed { index, picture ->
                    AnimatedVisibility(
                        visible = count > index && !(index == 1 && removed),
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut(),
                    ) {
                        Box(Modifier.size(56.dp + 8.dp)) {
                            Box(
                                Modifier
                                    .align(Alignment.BottomStart)
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(OcRadius.md))
                                    .background(picture)
                                    .border(BorderStroke(1.dp, c.borderBase), RoundedCornerShape(OcRadius.md))
                                    .target("thumb$index"),
                            )
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(c.bgInverse)
                                    .border(BorderStroke(1.dp, c.bgBase), CircleShape)
                                    .target("remove$index"),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Outlined.Close, contentDescription = null, tint = c.textInverse, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
            Text("Ask anything…", style = MaterialTheme.typography.bodyMedium, color = c.textFaint, modifier = Modifier.padding(vertical = OcSpacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Add, contentDescription = null, tint = c.iconBase)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Outlined.Mic, contentDescription = null, tint = c.iconBase)
                Spacer(Modifier.size(OcSpacing.md))
                Box(Modifier.size(32.dp).clip(CircleShape).background(c.bgAccent), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = c.onAccent, modifier = Modifier.size(18.dp))
                }
            }
        }
        // The full screen preview.
        AnimatedVisibility(preview, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize().background(Color(0xF0101010))) {
                Box(Modifier.align(Alignment.Center).fillMaxWidth().height(140.dp).background(PICTURES[0]))
                Text("1 / 2", style = MaterialTheme.typography.labelMedium, color = Color.White, modifier = Modifier.padding(OcSpacing.md))
                Box(
                    Modifier.align(Alignment.TopEnd).padding(OcSpacing.sm).size(32.dp).target("close"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = null, tint = Color.White)
                }
            }
        }
        finger.Draw()
    }
}
