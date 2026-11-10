package io.github.beppe2880.opencodemobile.navigation

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import io.github.beppe2880.opencodemobile.core.ui.platform.OcBackHandler
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.chat.SessionDrawerPull
import io.github.beppe2880.opencodemobile.feature.chat.drawerSettlesOpen
import io.github.beppe2880.opencodemobile.feature.sessions.SessionsRoute
import io.github.beppe2880.opencodemobile.shared.resources.Res
import io.github.beppe2880.opencodemobile.shared.resources.app_session_drawer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/** Share of the window width the chat list drawer takes, up to [DrawerMaxWidth]. */
private const val DRAWER_FRACTION = 0.85f
private val DrawerMaxWidth = 360.dp

/**
 * Per second: a release faster than this toward a side settles the drawer on that side, wherever
 * it is. Also used by the session list beside the chat on wide screens.
 */
internal val DrawerFlingVelocity = 400.dp

/** Stiffness of the spring that settles the drawer, and the list beside the chat, after a release. */
internal const val DRAWER_SETTLE_STIFFNESS = 600f

/**
 * Where the chat list drawer is: [position] goes from 0 (closed) to [width] (open), in pixels.
 * Drags move it with the finger; a release settles it open or closed by velocity, else by
 * whether it is past half its width.
 */
@Stable
internal class SessionDrawerState(private val scope: CoroutineScope, private val flingVelocity: Float) : SessionDrawerPull {
    var width by mutableFloatStateOf(0f)
        private set
    var position by mutableFloatStateOf(0f)
        private set

    /** Partly or fully on screen: the list is composed. */
    val visible by derivedStateOf { position > 0f }
    val isOpen by derivedStateOf { width > 0f && position >= width }

    /** Share of the drawer on screen, for the scrim. */
    val fraction: Float get() = if (width > 0f) (position / width).coerceIn(0f, 1f) else 0f

    private var settling: Job? = null

    override fun drag(delta: Float) {
        settling?.cancel()
        position = (position + delta).coerceIn(0f, width)
    }

    override fun release(velocity: Float) {
        val open = drawerSettlesOpen(position, width, velocity, flingVelocity)
        settle(if (open) width else 0f, velocity)
    }

    fun open() = settle(width)

    fun close() = settle(0f)

    /** A new window width: an open drawer stays open at its new width. */
    fun resize(newWidth: Float) {
        if (newWidth == width) return
        val wasOpen = isOpen
        width = newWidth
        position = if (wasOpen) newWidth else position.coerceIn(0f, newWidth)
    }

    private fun settle(target: Float, velocity: Float = 0f) {
        settling?.cancel()
        settling = scope.launch {
            animate(position, target, velocity, spring(stiffness = DRAWER_SETTLE_STIFFNESS)) { value, _ -> position = value }
        }
    }
}

/**
 * The chat with the session list in a drawer from the start edge (the end in RTL). The chat's
 * own swipe pulls it through the [SessionDrawerPull] handed to [content], so scrolling, text
 * selection and horizontal scrollers keep their gestures; the panel follows the finger and the
 * scrim fades in with it. Dragging the panel or the scrim back, tapping the scrim, Back or
 * picking a chat close it; [onOpenChat] gets the picked one unless it is [currentSessionId].
 * [sessionsOwner] keeps the list's ViewModel shared with the session list screen when it is in
 * the back stack.
 */
@Composable
internal fun SessionDrawerHost(
    currentSessionId: String,
    sessionsOwner: ViewModelStoreOwner?,
    demo: Boolean,
    onOpenChat: (String) -> Unit,
    onOpenSettings: () -> Unit,
    content: @Composable (drawer: SessionDrawerPull) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    // Not saved: a chat shown again (Back from Settings, recreation) starts with the list closed.
    val drawer = remember(density) { SessionDrawerState(scope, with(density) { DrawerFlingVelocity.toPx() }) }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val toEnd = if (rtl) -1f else 1f
    val sheetFocus = remember { FocusRequester() }
    val chatFocus = remember { FocusRequester() }
    var hadFocus by remember { mutableStateOf(false) }
    // Focus moves into the list once it is open and back to the chat once it is closed.
    LaunchedEffect(drawer.isOpen, drawer.visible) {
        if (drawer.isOpen) {
            hadFocus = true
            runCatching { sheetFocus.requestFocus() }
        } else if (!drawer.visible && hadFocus) {
            hadFocus = false
            runCatching { chatFocus.requestFocus() }
        }
    }
    val label = stringResource(Res.string.app_session_drawer)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val drawerWidth = (maxWidth * DRAWER_FRACTION).coerceAtMost(DrawerMaxWidth)
        val drawerWidthPx = with(density) { drawerWidth.toPx() }
        SideEffect { drawer.resize(drawerWidthPx) }
        Box(Modifier.fillMaxSize().focusRequester(chatFocus).focusable()) {
            content(drawer)
        }
        // Composed only while partly or fully on screen, so the list refreshes only then. The
        // drag sits on this box, which does not move, rather than on the moving panel.
        if (drawer.visible) Box(Modifier.fillMaxSize().drawerDrag(drawer, toEnd)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = drawer.fraction }
                    .background(OcTheme.colors.scrim)
                    .pointerInput(drawer) { detectTapGestures { drawer.close() } }
                    .semantics {
                        onClick {
                            drawer.close()
                            true
                        }
                    }
                    .testTag("chat_session_drawer_scrim"),
            )
            Surface(
                color = OcTheme.colors.bgBase,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .width(drawerWidth)
                    .fillMaxHeight()
                    // Mirrored in RTL by offset itself.
                    .offset { IntOffset((drawer.position - drawer.width).roundToInt(), 0) }
                    .semantics { paneTitle = label }
                    .focusRequester(sheetFocus)
                    .focusable()
                    .testTag("chat_session_drawer"),
            ) {
                val list: @Composable () -> Unit = {
                    SessionsRoute(
                        onOpenChat = { id ->
                            if (id == currentSessionId) drawer.close() else onOpenChat(id)
                        },
                        onOpenSettings = {
                            drawer.close()
                            onOpenSettings()
                        },
                        selectedSessionId = currentSessionId,
                        demo = demo,
                        inDrawer = true,
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Start)),
                    )
                }
                if (sessionsOwner != null) CompositionLocalProvider(LocalViewModelStoreOwner provides sessionsOwner, content = list) else list()
            }
        }
    }
    // After the chat, so it gets Back before the chat's own handlers while the list shows.
    OcBackHandler(enabled = drawer.visible) { drawer.close() }
}

/**
 * Horizontal drags over the drawer or its scrim move it; [toEnd] is the sign of a move toward
 * the end edge. Must sit on a node that does not move with the drawer.
 */
private fun Modifier.drawerDrag(drawer: SessionDrawerState, toEnd: Float): Modifier = pointerInput(drawer, toEnd) {
    val velocity = VelocityTracker()
    detectHorizontalDragGestures(
        onDragStart = { velocity.resetTracking() },
        onDragEnd = { drawer.release(velocity.calculateVelocity().x * toEnd) },
        onDragCancel = { drawer.release(0f) },
        onHorizontalDrag = { change, delta ->
            change.consume()
            velocity.addPosition(change.uptimeMillis, change.position)
            drawer.drag(delta * toEnd)
        },
    )
}
