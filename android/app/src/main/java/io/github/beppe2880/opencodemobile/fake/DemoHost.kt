package io.github.beppe2880.opencodemobile.fake

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.beppe2880.opencodemobile.R
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import io.github.beppe2880.opencodemobile.core.ui.components.OcButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcButtonVariant
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.core.ui.theme.isDark
import io.github.beppe2880.opencodemobile.navigation.AppNavHost
import org.koin.compose.KoinIsolatedContext
import org.koin.core.KoinApplication

/** No real repositories, stores, network clients, widgets or notification monitors enter this container. */
@Composable
fun DemoHost(container: KoinApplication, onExit: () -> Unit, realDarkTheme: Boolean, modifier: Modifier = Modifier) {
    val settings = remember(container) { container.koin.get<SettingsRepository>() }
    val preferences by settings.settings.collectAsStateWithLifecycle()
    val dark = preferences.theme.isDark()
    val activity = LocalActivity.current as? ComponentActivity
    DisposableEffect(activity, dark, realDarkTheme) {
        fun applyStyle(isDark: Boolean) {
            val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { isDark }
            activity?.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        }
        applyStyle(dark)
        onDispose { applyStyle(realDarkTheme) }
    }
    KoinIsolatedContext(container) {
        OcTheme(darkTheme = dark) {
            // Destination BackHandlers take precedence; back from the demo root exits it.
            BackHandler(onBack = onExit)
            Column(
                modifier.fillMaxSize().background(OcTheme.colors.bgBase)
                    .statusBarsPadding().consumeWindowInsets(WindowInsets.statusBars),
            ) {
                Row(
                    Modifier.background(OcTheme.colors.bgDeep).padding(horizontal = OcSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.app_demo_label),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelMedium,
                        color = OcTheme.colors.textMuted,
                    )
                    OcButton(
                        text = stringResource(R.string.app_demo_exit),
                        onClick = onExit,
                        variant = OcButtonVariant.Ghost,
                        modifier = Modifier.testTag("demo_exit"),
                    )
                }
                AppNavHost(
                    isDemo = true,
                    hasActiveServer = true,
                    modifier = Modifier.weight(1f),
                    onConnectServer = onExit,
                )
            }
        }
    }
}
