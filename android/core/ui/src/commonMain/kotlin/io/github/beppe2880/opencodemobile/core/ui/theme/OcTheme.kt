package io.github.beppe2880.opencodemobile.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.beppe2880.opencodemobile.core.domain.ThemeMode

/** 4dp based spacing scale. */
@Immutable
object OcSpacing {
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
    val xxxl: Dp = 48.dp

    /** Horizontal gutter for screen content. */
    val gutter: Dp = 16.dp

    /** Max readable width of single-pane content on large screens. */
    val contentMaxWidth: Dp = 760.dp
}

@Immutable
object OcRadius {
    val xs: Dp = 4.dp
    val sm: Dp = 6.dp
    val md: Dp = 8.dp
    val lg: Dp = 10.dp
    val xl: Dp = 14.dp
    val sheet: Dp = 16.dp
}

val LocalOcColors = staticCompositionLocalOf { OcLightColors }

/** IBM Plex Mono styles; provided by [OcTheme] because resource fonts load in composition. */
val LocalOcMono = staticCompositionLocalOf {
    OcMonoTypography(
        code = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 19.sp),
        codeSmall = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 17.sp),
        label = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    )
}

/** Entry point for OpenCode tokens beyond Material's scheme. */
object OcTheme {
    val colors: OcColors
        @Composable @ReadOnlyComposable
        get() = LocalOcColors.current
    val mono: OcMonoTypography
        @Composable @ReadOnlyComposable
        get() = LocalOcMono.current

    /** The markdown colors of the active OpenCode theme; null renders markdown uncolored. */
    val markdown: OcMarkdownPalette?
        @Composable @ReadOnlyComposable
        get() = LocalOcColors.current.markdown
    val spacing: OcSpacing get() = OcSpacing
    val radius: OcRadius get() = OcRadius
}

@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}

/**
 * The app theme. [openCodeTheme] is the id of an OpenCode terminal app theme ([OpenCodeThemes]):
 * the whole app then takes its colors in the [darkTheme] mode, or its only mode, and
 * [OcColors.isDark] (also the Material scheme) follows the theme rather than [darkTheme]. Null or
 * an unknown id keeps the app's own colors.
 */
@Composable
fun OcTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    openCodeTheme: String? = null,
    content: @Composable () -> Unit,
) {
    val colors = remember(openCodeTheme, darkTheme) { ocColors(openCodeTheme, darkTheme) }
    CompositionLocalProvider(LocalOcColors provides colors, LocalOcMono provides ocMonoTypography()) {
        MaterialTheme(
            colorScheme = colors.toColorScheme(),
            typography = ocMaterialTypography(),
            shapes = OcShapes,
            content = content,
        )
    }
}

private val OcShapes = Shapes(
    extraSmall = RoundedCornerShape(OcRadius.xs),
    small = RoundedCornerShape(OcRadius.sm),
    medium = RoundedCornerShape(OcRadius.md),
    large = RoundedCornerShape(OcRadius.lg),
    extraLarge = RoundedCornerShape(OcRadius.sheet),
)

private fun OcColors.toColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = bgAccent,
        onPrimary = onAccent,
        primaryContainer = info.bg,
        onPrimaryContainer = info.fg,
        inversePrimary = textAccent,
        secondary = textMuted,
        onSecondary = bgBase,
        secondaryContainer = layer02,
        onSecondaryContainer = textBase,
        tertiary = agents.plan,
        onTertiary = bgBase,
        tertiaryContainer = layer02,
        onTertiaryContainer = textBase,
        background = bgDeep,
        onBackground = textBase,
        surface = bgBase,
        onSurface = textBase,
        surfaceVariant = layer02,
        onSurfaceVariant = textMuted,
        surfaceTint = bgAccent,
        inverseSurface = bgInverse,
        inverseOnSurface = textInverse,
        error = danger.fg,
        onError = bgBase,
        errorContainer = danger.bg,
        onErrorContainer = danger.fg,
        outline = borderStrong,
        outlineVariant = borderBase,
        scrim = scrim,
        surfaceBright = bgBase,
        surfaceDim = layer02,
        surfaceContainerLowest = bgBase,
        surfaceContainerLow = bgBase,
        surfaceContainer = layer01,
        surfaceContainerHigh = if (isDark) layer01 else bgBase,
        surfaceContainerHighest = layer02,
    )
}
