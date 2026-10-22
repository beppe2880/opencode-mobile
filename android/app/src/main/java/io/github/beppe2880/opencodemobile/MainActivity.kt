package io.github.beppe2880.opencodemobile

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.beppe2880.opencodemobile.core.domain.ServerRepository
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.core.ui.theme.isDark
import io.github.beppe2880.opencodemobile.navigation.AppNavHost
import io.github.beppe2880.opencodemobile.notifications.NotificationsIntro
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val servers: ServerRepository by inject()
    private val settings: SettingsRepository by inject()

    /** Session to open, from a tapped notification or widget; cleared once navigated. */
    private val sessionToOpen = MutableStateFlow<String?>(null)

    /** The widget's new session button was tapped; cleared once the sheet is open. */
    private val newSessionRequested = MutableStateFlow(false)

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) readIntent(intent)
        keepSplashUntilLoaded()
        setContent {
            val appSettings by settings.settings.collectAsStateWithLifecycle()
            val loaded by servers.loaded.collectAsStateWithLifecycle()
            val activeServer by servers.activeServer.collectAsStateWithLifecycle()
            val openSession by sessionToOpen.collectAsStateWithLifecycle()
            val newSession by newSessionRequested.collectAsStateWithLifecycle()
            val dark = appSettings.theme.isDark()
            DisposableEffect(dark) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }
            OcTheme(darkTheme = dark) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(OcTheme.colors.bgBase)
                        .semantics { testTagsAsResourceId = true },
                ) {
                    // Choosing the start screen needs the saved servers, read off the main thread.
                    if (loaded) {
                        AppNavHost(
                            hasActiveServer = activeServer != null,
                            openSessionId = openSession,
                            onSessionOpened = { sessionToOpen.value = null },
                            openNewSession = newSession,
                            onNewSessionOpened = { newSessionRequested.value = false },
                        )
                        if (activeServer != null && !appSettings.notificationsIntroShown) NotificationsIntro(settings)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readIntent(intent)
    }

    /** The launcher activity is exported: accept only a well-formed session id. */
    private fun readIntent(intent: Intent?) {
        intent ?: return
        if (intent.getBooleanExtra(EXTRA_NEW_SESSION, false)) {
            intent.removeExtra(EXTRA_NEW_SESSION)
            newSessionRequested.value = true
        }
        val id = intent.getStringExtra(EXTRA_SESSION_ID) ?: return
        intent.removeExtra(EXTRA_SESSION_ID)
        if (SESSION_ID.matches(id)) sessionToOpen.value = id
    }

    companion object {
        const val EXTRA_SESSION_ID = "io.github.beppe2880.opencodemobile.extra.SESSION_ID"
        const val EXTRA_NEW_SESSION = "io.github.beppe2880.opencodemobile.extra.NEW_SESSION"
        private val SESSION_ID = Regex("ses_[A-Za-z0-9]{1,64}")
    }

    /** Holds the first frame (the system splash screen) until the saved servers are read. */
    private fun keepSplashUntilLoaded() {
        if (servers.loaded.value) return
        val content = findViewById<View>(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (!servers.loaded.value) return false
                    content.viewTreeObserver.removeOnPreDrawListener(this)
                    return true
                }
            },
        )
    }
}
