package io.github.beppe2880.opencodemobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.ServerRepository
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.core.ui.theme.isDark
import io.github.beppe2880.opencodemobile.navigation.AppNavHost
import org.koin.compose.koinInject

/**
 * The app shell: theme, start screen and navigation. [overlay] is drawn above the navigation
 * while a server is active (Android shows the notifications intro there).
 */
@Composable
fun OpenCodeApp(
    modifier: Modifier = Modifier,
    openSessionId: String? = null,
    onSessionOpened: () -> Unit = {},
    /** Opens the new session sheet over the session list (Android widget button). */
    openNewSession: Boolean = false,
    onNewSessionOpened: () -> Unit = {},
    onDarkThemeChange: (Boolean) -> Unit = {},
    onTryDemo: (() -> Unit)? = null,
    overlay: @Composable (AppSettings) -> Unit = {},
) {
    val settings = koinInject<SettingsRepository>()
    val servers = koinInject<ServerRepository>()
    val appSettings by settings.settings.collectAsStateWithLifecycle()
    val loaded by servers.loaded.collectAsStateWithLifecycle()
    val activeServer by servers.activeServer.collectAsStateWithLifecycle()
    val dark = appSettings.theme.isDark()
    LaunchedEffect(dark) { onDarkThemeChange(dark) }
    OcTheme(darkTheme = dark) {
        Box(modifier.fillMaxSize().background(OcTheme.colors.bgBase).dismissKeyboardOnTap()) {
            // Choosing the start screen needs the saved servers, read off the main thread.
            if (loaded) {
                AppNavHost(
                    onTryDemo = onTryDemo,
                    hasActiveServer = activeServer != null,
                    openSessionId = openSessionId,
                    onSessionOpened = onSessionOpened,
                    openNewSession = openNewSession,
                    onNewSessionOpened = onNewSessionOpened,
                )
                if (activeServer != null) overlay(appSettings)
            }
        }
    }
}
