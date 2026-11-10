package io.github.beppe2880.opencodemobile.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Foreground, background and border triple for a semantic state (success, warning, ...). */
@Immutable
data class OcStateColors(
    val fg: Color,
    val bg: Color,
    val border: Color,
)

/** Agent accent colors, matching the OpenCode web UI. */
@Immutable
data class OcAgentColors(
    val build: Color,
    val plan: Color,
    val explore: Color,
    val review: Color,
    val writer: Color,
) {
    private val palette get() = listOf(build, plan, explore, review, writer)

    /** Known agents map to their named color, others to a stable palette slot. */
    fun forAgent(name: String?): Color = when (name?.lowercase()) {
        null, "", "build" -> build
        "plan" -> plan
        "explore" -> explore
        "review" -> review
        "writer" -> writer
        else -> palette[(name.hashCode() and Int.MAX_VALUE) % palette.size]
    }
}

/**
 * Colors of a unified diff. The app's own themes derive them from the state colors; an OpenCode
 * theme gives its `diff` tokens.
 */
@Immutable
data class OcDiffColors(
    /** The `+` marker of an added line. */
    val added: Color,
    /** The `-` marker of a removed line. */
    val removed: Color,
    /** The code of every line. */
    val text: Color,
    /** The text of a hunk header (`@@ -1,4 +1,5 @@`). */
    val hunkText: Color,
    val addedBackground: Color,
    val removedBackground: Color,
    val contextBackground: Color,
    val hunkBackground: Color,
    val lineNumber: Color,
    /** Behind the line numbers of an added line; the line's own background in the app's themes. */
    val lineNumberAddedBackground: Color,
    val lineNumberRemovedBackground: Color,
)

/** Syntax colors of code, the `syntax` tokens of an OpenCode theme. */
@Immutable
data class OcSyntaxColors(
    val comment: Color,
    val keyword: Color,
    val function: Color,
    val variable: Color,
    val string: Color,
    val number: Color,
    val type: Color,
    val operator: Color,
    val punctuation: Color,
)

/** Full OpenCode V2 token set. Use through [OcTheme.colors]. */
@Immutable
data class OcColors(
    val isDark: Boolean,
    val bgDeep: Color,
    val bgBase: Color,
    val layer01: Color,
    val layer02: Color,
    val layer03: Color,
    val layer04: Color,
    val bgInverse: Color,
    val bgAccent: Color,
    val onAccent: Color,
    val textBase: Color,
    val textMuted: Color,
    val textFaint: Color,
    val textAccent: Color,
    val textInverse: Color,
    val textCodePath: Color,
    val bgCodePath: Color,
    val borderBase: Color,
    val borderMuted: Color,
    val borderStrong: Color,
    val borderFocus: Color,
    val iconBase: Color,
    val iconMuted: Color,
    val userBubbleBg: Color,
    val userBubbleText: Color,
    val scrim: Color,
    val success: OcStateColors,
    val warning: OcStateColors,
    val danger: OcStateColors,
    val info: OcStateColors,
    val agents: OcAgentColors,
    /** Diff colors; by default the state colors the app always used. */
    val diff: OcDiffColors = OcDiffColors(
        added = success.fg,
        removed = danger.fg,
        text = textBase,
        hunkText = textFaint,
        addedBackground = success.bg,
        removedBackground = danger.bg,
        contextBackground = layer01,
        hunkBackground = layer02,
        lineNumber = textFaint,
        lineNumberAddedBackground = success.bg,
        lineNumberRemovedBackground = danger.bg,
    ),
    /**
     * The markdown colors of an OpenCode theme; null (the app's own themes) renders markdown in the
     * text color.
     */
    val markdown: OcMarkdownPalette? = null,
    /** The syntax colors of an OpenCode theme; null highlights code with the Atom One theme. */
    val syntax: OcSyntaxColors? = null,
    /** The 16 ANSI colors of an OpenCode theme; null uses the app's sets, see [terminalAnsi]. */
    val ansi: List<Color>? = null,
)

val OcLightColors = OcColors(
    isDark = false,
    bgDeep = Color(0xFFFAFAFA),
    bgBase = Color(0xFFFFFFFF),
    layer01 = Color(0xFFFAFAFA),
    layer02 = Color(0xFFF2F2F2),
    layer03 = Color(0xFFEEEEEE),
    layer04 = Color(0xFFDBDBDB),
    bgInverse = Color(0xFF161616),
    bgAccent = Color(0xFF3B5CF6),
    onAccent = Color(0xFFFFFFFF),
    textBase = Color(0xFF161616),
    textMuted = Color(0xFF5C5C5C),
    textFaint = Color(0xFF808080),
    textAccent = Color(0xFF3B5CF6),
    textInverse = Color(0xFFFAFAFA),
    textCodePath = Color(0xFF2C47C8),
    bgCodePath = Color(0xFFE2F7FB),
    borderBase = Color(0x1A000000),
    borderMuted = Color(0x14000000),
    borderStrong = Color(0x33000000),
    borderFocus = Color(0xFF7698FD),
    iconBase = Color(0xFF3A3A3A),
    iconMuted = Color(0xFF808080),
    userBubbleBg = Color(0xFFECF1FE),
    userBubbleText = Color(0xFF2C47C8),
    scrim = Color(0x66000000),
    success = OcStateColors(Color(0xFF198B43), Color(0xFFE7F9EA), Color(0xFFB8E9C1)),
    warning = OcStateColors(Color(0xFF8F6E1E), Color(0xFFFEFAEC), Color(0xFFF7E5B5)),
    danger = OcStateColors(Color(0xFFB82D35), Color(0xFFFCECEB), Color(0xFFF2BBB7)),
    info = OcStateColors(Color(0xFF3250DF), Color(0xFFECF1FE), Color(0xFFC3D4FD)),
    agents = OcAgentColors(
        build = Color(0xFF3250DF),
        plan = Color(0xFFC83D8B),
        explore = Color(0xFFAC8833),
        review = Color(0xFF198B43),
        writer = Color(0xFF623BE2),
    ),
)

val OcDarkColors = OcColors(
    isDark = true,
    bgDeep = Color(0xFF080808),
    bgBase = Color(0xFF161616),
    layer01 = Color(0xFF242424),
    layer02 = Color(0xFF2E2E2E),
    layer03 = Color(0xFF3A3A3A),
    layer04 = Color(0xFF5C5C5C),
    bgInverse = Color(0xFFFFFFFF),
    bgAccent = Color(0xFF3B5CF6),
    onAccent = Color(0xFFFFFFFF),
    textBase = Color(0xFFFAFAFA),
    textMuted = Color(0xFFAEAEAE),
    textFaint = Color(0xFF808080),
    textAccent = Color(0xFFA2BCFF),
    textInverse = Color(0xFF161616),
    textCodePath = Color(0xFFC3D4FD),
    bgCodePath = Color(0xFF00353F),
    borderBase = Color(0x1AFFFFFF),
    borderMuted = Color(0x14FFFFFF),
    borderStrong = Color(0x33FFFFFF),
    borderFocus = Color(0xFF7698FD),
    iconBase = Color(0xFFDBDBDB),
    iconMuted = Color(0xFF808080),
    userBubbleBg = Color(0xFF021945),
    userBubbleText = Color(0xFFC3D4FD),
    scrim = Color(0x99000000),
    success = OcStateColors(Color(0xFF6BD586), Color(0xFF14361D), Color(0xFF1D783C)),
    warning = OcStateColors(Color(0xFFF2CF76), Color(0xFF4B4025), Color(0xFFAC8833)),
    danger = OcStateColors(Color(0xFFF17471), Color(0xFF461516), Color(0xFF97252B)),
    info = OcStateColors(Color(0xFF7698FD), Color(0xFF021945), Color(0xFF2C47C8)),
    agents = OcAgentColors(
        build = Color(0xFFC3D4FD),
        plan = Color(0xFFF799C6),
        explore = Color(0xFFF7E5B5),
        review = Color(0xFFB8E9C1),
        writer = Color(0xFF9E99F7),
    ),
)
