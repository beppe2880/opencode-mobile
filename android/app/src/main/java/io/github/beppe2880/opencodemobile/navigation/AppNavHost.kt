package io.github.beppe2880.opencodemobile.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.beppe2880.opencodemobile.R
import io.github.beppe2880.opencodemobile.core.ui.components.OcEmptyState
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.chat.ChatRoute
import io.github.beppe2880.opencodemobile.feature.connect.ConnectRoute
import io.github.beppe2880.opencodemobile.feature.sessions.SessionsRoute
import io.github.beppe2880.opencodemobile.feature.settings.SettingsRoute
import kotlinx.serialization.Serializable

@Serializable
data class ConnectDestination(val addServer: Boolean = false)

@Serializable
data object SessionsDestination

@Serializable
data class ChatDestination(val sessionId: String)

@Serializable
data object SettingsDestination

/** Width at which Sessions and Chat are shown side by side. */
private val ListDetailMinWidth = 840.dp
private val ListPaneWidth = 360.dp

@Composable
fun AppNavHost(
    hasActiveServer: Boolean,
    modifier: Modifier = Modifier,
    openSessionId: String? = null,
    onSessionOpened: () -> Unit = {},
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

    NavHost(navController = navController, startDestination = start, modifier = modifier) {
        composable<ConnectDestination> { entry ->
            val args = entry.toRoute<ConnectDestination>()
            ConnectRoute(
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
                onOpenChatFullScreen = { navController.navigate(ChatDestination(it)) },
                onOpenSettings = { navController.navigate(SettingsDestination) },
            )
        }
        composable<ChatDestination> { entry ->
            val args = entry.toRoute<ChatDestination>()
            ChatRoute(
                sessionId = args.sessionId,
                onBack = { navController.popBackStack() },
                onOpenSession = { navController.navigate(ChatDestination(it)) },
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
                onBack = { navController.popBackStack() },
                onAddServer = { navController.navigate(ConnectDestination(addServer = true)) },
            )
        }
    }
}

/** Phone: sessions list only. Expanded width: list + chat detail side by side. */
@Composable
private fun SessionsListDetail(
    onOpenChatFullScreen: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth < ListDetailMinWidth) {
            SessionsRoute(onOpenChat = onOpenChatFullScreen, onOpenSettings = onOpenSettings)
        } else {
            Row(Modifier.fillMaxSize()) {
                SessionsRoute(
                    onOpenChat = { selected = it },
                    onOpenSettings = onOpenSettings,
                    selectedSessionId = selected,
                    modifier = Modifier.width(ListPaneWidth).fillMaxHeight(),
                )
                VerticalDivider(color = OcTheme.colors.borderBase)
                val current = selected
                if (current != null) {
                    ChatRoute(
                        sessionId = current,
                        onBack = { selected = null },
                        showBack = false,
                        onOpenSession = onOpenChatFullScreen,
                        onSwitchSession = { selected = it },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Box(
                        Modifier.weight(1f).fillMaxHeight().background(OcTheme.colors.bgBase),
                        contentAlignment = Alignment.Center,
                    ) {
                        OcEmptyState(
                            icon = Icons.Outlined.ChatBubbleOutline,
                            title = stringResource(R.string.app_select_session),
                            message = stringResource(R.string.app_select_session_message),
                        )
                    }
                }
            }
        }
    }
}
