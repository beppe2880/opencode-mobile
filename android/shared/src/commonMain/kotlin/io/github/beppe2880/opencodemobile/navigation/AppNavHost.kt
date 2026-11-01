package io.github.beppe2880.opencodemobile.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.beppe2880.opencodemobile.core.domain.DEFAULT_LIST_PANE_FRACTION
import io.github.beppe2880.opencodemobile.core.domain.ServerRepository
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import io.github.beppe2880.opencodemobile.core.ui.components.OcEmptyState
import io.github.beppe2880.opencodemobile.core.ui.platform.OcBackHandler
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.chat.ChatRoute
import io.github.beppe2880.opencodemobile.feature.connect.ConnectRoute
import io.github.beppe2880.opencodemobile.feature.files.FileViewerRoute
import io.github.beppe2880.opencodemobile.feature.sessions.SessionsRoute
import io.github.beppe2880.opencodemobile.feature.settings.SettingsRoute
import io.github.beppe2880.opencodemobile.feature.settings.update.UpdateNoticeRoute
import io.github.beppe2880.opencodemobile.feature.settings.usage.UsageRoute
import io.github.beppe2880.opencodemobile.feature.terminal.TerminalRoute
import io.github.beppe2880.opencodemobile.shared.resources.Res
import io.github.beppe2880.opencodemobile.shared.resources.app_hide_sessions
import io.github.beppe2880.opencodemobile.shared.resources.app_select_session
import io.github.beppe2880.opencodemobile.shared.resources.app_select_session_message
import io.github.beppe2880.opencodemobile.shared.resources.app_show_sessions
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Serializable
data class ConnectDestination(val addServer: Boolean = false)

@Serializable
data object SessionsDestination

@Serializable
data class ChatDestination(val sessionId: String)

@Serializable
data object SettingsDestination

@Serializable
data object UsageDestination

@Serializable
data class TerminalDestination(val directory: String)

@Serializable
data class FileDestination(val directory: String, val path: String)

/**
 * Width at which Sessions and Chat are shown side by side: medium windows and up, so unfolded
 * foldables (about 670-820 dp) get both panes, not only tablets.
 */
private val ListDetailMinWidth = 600.dp

/** By default the list takes [DEFAULT_LIST_PANE_FRACTION] of the width, between the bounds, leaving the chat the most room. */
private val ListPaneDefaultMinWidth = 280.dp
private val ListPaneDefaultMaxWidth = 360.dp

/** Bounds when the list is resized by dragging the handle; dropping it narrower hides it. */
private val ListPaneMinWidth = 240.dp
private val ChatMinWidth = 320.dp

@Composable
fun AppNavHost(
    hasActiveServer: Boolean,
    modifier: Modifier = Modifier,
    openSessionId: String? = null,
    onSessionOpened: () -> Unit = {},
    /** Opens the new session sheet over the session list (widget button). */
    openNewSession: Boolean = false,
    onNewSessionOpened: () -> Unit = {},
    onTryDemo: (() -> Unit)? = null,
    onConnectServer: (() -> Unit)? = null,
    isDemo: Boolean = false,
    navController: NavHostController = rememberNavController(),
) {
    val start: Any = if (hasActiveServer) SessionsDestination else ConnectDestination()

    // Losing the active server (removed in Settings) always returns to Connect with a clean stack.
    LaunchedEffect(hasActiveServer) {
        if (!hasActiveServer && navController.currentDestination?.hasRoute<ConnectDestination>() != true) {
            navController.navigate(ConnectDestination()) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    // A tapped notification opens its chat above the session list, replacing other screens.
    LaunchedEffect(openSessionId, hasActiveServer) {
        if (openSessionId == null || !hasActiveServer) return@LaunchedEffect
        navController.navigate(ChatDestination(openSessionId)) {
            popUpTo<SessionsDestination>()
        }
        onSessionOpened()
    }

    // Handed to the session list, which opens its sheet and clears it.
    var pendingNewSession by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(openNewSession, hasActiveServer) {
        if (!openNewSession || !hasActiveServer) return@LaunchedEffect
        navController.popBackStack(SessionsDestination, inclusive = false)
        pendingNewSession = true
        onNewSessionOpened()
    }

    NavHost(navController = navController, startDestination = start, modifier = modifier) {
        composable<ConnectDestination> { entry ->
            val args = entry.toRoute<ConnectDestination>()
            ConnectRoute(
                // Only for a first connection: "Add server" from Settings is for real servers.
                onTryDemo = onTryDemo.takeUnless { args.addServer },
                showBack = args.addServer,
                onBack = { navController.popBackStack() },
                onConnected = {
                    if (args.addServer) {
                        navController.popBackStack(SessionsDestination, inclusive = false)
                    } else {
                        navController.navigate(SessionsDestination) {
                            popUpTo<ConnectDestination> { inclusive = true }
                        }
                    }
                },
            )
        }
        composable<SessionsDestination> {
            SessionsListDetail(
                demo = isDemo,
                onOpenChatFullScreen = navController::openChat,
                onOpenSettings = { navController.navigate(SettingsDestination) },
                openNewSession = pendingNewSession,
                onNewSessionOpened = { pendingNewSession = false },
                onOpenTerminal = { navController.navigate(TerminalDestination(it)) },
                onOpenFile = { directory, path -> navController.navigate(FileDestination(directory, path)) },
            )
        }
        composable<ChatDestination> { entry ->
            val args = entry.toRoute<ChatDestination>()
            ChatRoute(
                sessionId = args.sessionId,
                onBack = { navController.popBackStack() },
                onOpenSession = navController::openChat,
                onOpenTerminal = { navController.navigate(TerminalDestination(it)) },
                onOpenFile = { directory, path -> navController.navigate(FileDestination(directory, path)) },
                onSwitchSession = {
                    // Replaces this chat, so Back still returns to the session list.
                    navController.navigate(ChatDestination(it)) {
                        popUpTo<ChatDestination> { inclusive = true }
                    }
                },
            )
        }
        composable<SettingsDestination> {
            SettingsRoute(
                demo = isDemo,
                onBack = { navController.popBackStack() },
                onAddServer = onConnectServer ?: { navController.navigate(ConnectDestination(addServer = true)) },
                onOpenUsage = { navController.navigate(UsageDestination) },
                preview = settingsPreviews,
            )
        }
        composable<UsageDestination> {
            UsageRoute(onBack = { navController.popBackStack() })
        }
        composable<TerminalDestination> { entry ->
            TerminalRoute(directory = entry.toRoute<TerminalDestination>().directory, onBack = { navController.popBackStack() })
        }
        composable<FileDestination> { entry ->
            val args = entry.toRoute<FileDestination>()
            FileViewerRoute(directory = args.directory, path = args.path, onBack = { navController.popBackStack() })
        }
    }
}

/**
 * Pushes the chat of [sessionId] unless it is already on top: a repeated tap (or a second
 * effect delivered during the transition) would otherwise stack it twice, needing an extra Back.
 */
private fun NavController.openChat(sessionId: String) {
    val top = currentBackStackEntry
    if (top?.destination?.hasRoute<ChatDestination>() == true && top.toRoute<ChatDestination>().sessionId == sessionId) return
    navigate(ChatDestination(sessionId))
}

/**
 * Phone: sessions list only. Expanded width (unless turned off in Settings): list + chat detail
 * side by side. Tapping the handle on the divider hides the list to give the chat the whole
 * width; dragging it resizes the list, and the share of the width is saved. Back shows a hidden
 * list again (unless turned off in Settings) before it leaves the app.
 */
@Composable
private fun SessionsListDetail(
    demo: Boolean,
    onOpenChatFullScreen: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTerminal: (String) -> Unit,
    onOpenFile: (String, String) -> Unit,
    openNewSession: Boolean,
    onNewSessionOpened: () -> Unit,
) {
    val settingsRepository = koinInject<SettingsRepository>()
    val appSettings by settingsRepository.settings.collectAsStateWithLifecycle()
    val activeServerId = koinInject<ServerRepository>().activeServer.collectAsStateWithLifecycle().value?.id
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    // The server [selected] belongs to: after switching servers the chat pane starts empty.
    var selectedServerId by rememberSaveable { mutableStateOf<String?>(null) }
    val select: (String?) -> Unit = {
        selected = it
        selectedServerId = activeServerId
    }
    var listHidden by rememberSaveable { mutableStateOf(false) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth < ListDetailMinWidth || !appSettings.splitView) {
            SessionsRoute(
                onOpenChat = onOpenChatFullScreen,
                onOpenSettings = onOpenSettings,
                openNewSession = openNewSession,
                onNewSessionOpened = onNewSessionOpened,
                banner = { UpdateBanner() },
                demo = demo,
            )
        } else {
            val windowWidth = maxWidth
            val maxListWidth = (windowWidth - ChatMinWidth).coerceAtLeast(ListPaneMinWidth)
            // Applied at once on release; the saved value catches up through the settings flow.
            var fraction by remember(appSettings.listPaneFraction) { mutableStateOf(appSettings.listPaneFraction) }
            val shownWidth = fraction?.let { (windowWidth * it).coerceIn(ListPaneMinWidth, maxListWidth) }
                ?: (windowWidth * DEFAULT_LIST_PANE_FRACTION).coerceIn(ListPaneDefaultMinWidth, ListPaneDefaultMaxWidth)
            // Without the handle there is no way to bring a hidden list back.
            val hidden = listHidden && appSettings.listPaneHandle
            val target = if (hidden) 0.dp else shownWidth
            val animatedWidth = remember { Animatable(target, Dp.VectorConverter) }
            var dragWidth by remember { mutableStateOf<Dp?>(null) }
            LaunchedEffect(target) { animatedWidth.animateTo(target) }
            val listWidth = dragWidth ?: animatedWidth.value
            val scope = rememberCoroutineScope()
            val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
            val density = LocalDensity.current
            val current = selected?.takeIf { selectedServerId == activeServerId }
            // Registered before the panes, so handlers inside the chat still get Back first.
            OcBackHandler(enabled = hidden && appSettings.backShowsList) { listHidden = false }
            Row(Modifier.fillMaxSize()) {
                // Kept composed while hidden, so the list keeps its scroll position.
                SessionsRoute(
                    onOpenChat = select,
                    onOpenSettings = onOpenSettings,
                    selectedSessionId = current,
                    openNewSession = openNewSession,
                    onNewSessionOpened = {
                        listHidden = false
                        onNewSessionOpened()
                    },
                    modifier = Modifier.width(listWidth).fillMaxHeight().clipToBounds(),
                    banner = { UpdateBanner() },
                    demo = demo,
                )
                VerticalDivider(color = OcTheme.colors.borderBase)
                if (current != null) {
                    ChatRoute(
                        sessionId = current,
                        onBack = { select(null) },
                        showBack = false,
                        onOpenSession = onOpenChatFullScreen,
                        onOpenTerminal = onOpenTerminal,
                        onOpenFile = onOpenFile,
                        onSwitchSession = select,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Box(
                        Modifier.weight(1f).fillMaxHeight().background(OcTheme.colors.bgBase),
                        contentAlignment = Alignment.Center,
                    ) {
                        OcEmptyState(
                            icon = Icons.Outlined.ChatBubbleOutline,
                            title = stringResource(Res.string.app_select_session),
                            message = stringResource(Res.string.app_select_session_message),
                        )
                    }
                }
            }
            // Drawn over both panes so it can straddle the divider and still receive taps.
            if (appSettings.listPaneHandle) ListPaneHandle(
                listHidden = hidden,
                onToggle = { listHidden = !listHidden },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (listWidth - HandleWidth / 2).coerceAtLeast(0.dp))
                    .pointerInput(windowWidth, rtl) {
                        detectHorizontalDragGestures(
                            onDragStart = { dragWidth = animatedWidth.value },
                            onDragCancel = { dragWidth = null },
                            onDragEnd = {
                                val dropped = dragWidth ?: return@detectHorizontalDragGestures
                                scope.launch {
                                    // Continue from where the finger left the list, without a jump.
                                    animatedWidth.snapTo(dropped)
                                    dragWidth = null
                                    if (dropped < ListPaneMinWidth / 2) {
                                        listHidden = true
                                    } else {
                                        listHidden = false
                                        val chosen = dropped.coerceIn(ListPaneMinWidth, maxListWidth) / windowWidth
                                        fraction = chosen
                                        settingsRepository.update { it.copy(listPaneFraction = chosen) }
                                    }
                                }
                            },
                        ) { change, amount ->
                            change.consume()
                            val delta = with(density) { amount.toDp() } * if (rtl) -1f else 1f
                            dragWidth = ((dragWidth ?: animatedWidth.value) + delta).coerceIn(0.dp, maxListWidth)
                        }
                    },
            )
        }
    }
}

@Composable
private fun ListPaneHandle(listHidden: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(OcRadius.md),
        color = c.layer02,
        border = BorderStroke(1.dp, c.borderBase),
        modifier = modifier.size(width = HandleWidth, height = 48.dp).testTag("sessions_pane_toggle"),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (listHidden) Icons.AutoMirrored.Outlined.KeyboardArrowRight else Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = stringResource(if (listHidden) Res.string.app_show_sessions else Res.string.app_hide_sessions),
                tint = c.iconBase,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private val HandleWidth = 24.dp

/** The notice of a new app release, at the top of the session list. */
@Composable
private fun UpdateBanner() {
    UpdateNoticeRoute(Modifier.padding(start = OcSpacing.gutter, end = OcSpacing.gutter, top = OcSpacing.md))
}
