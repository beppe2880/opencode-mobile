package io.github.beppe2880.opencodemobile.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp

/** Opacity of a state color over the background for its tinted background (success, warning, ...). */
internal const val STATE_BACKGROUND_ALPHA = 0.15f

/** Opacity of a state color over the background for its border. */
internal const val STATE_BORDER_ALPHA = 0.45f

/**
 * The app colors: [OcLightColors] or [OcDarkColors] when [openCodeTheme] is null or unknown,
 * otherwise the colors of that OpenCode theme in the requested mode. The result's
 * [OcColors.isDark] is the theme's own: a theme with only a dark mode stays dark when light is
 * requested, and system bars must follow it, not [dark].
 */
fun ocColors(openCodeTheme: String?, dark: Boolean): OcColors =
    OpenCodeThemes.byId(openCodeTheme)?.tokens(dark)?.toOcColors() ?: if (dark) OcDarkColors else OcLightColors

/**
 * Whether the app is dark with [openCodeTheme] when [dark] is requested: for the system bar icons,
 * outside [OcTheme] (inside it, read `OcTheme.colors.isDark`).
 */
fun isDarkTheme(openCodeTheme: String?, dark: Boolean): Boolean = OpenCodeThemes.byId(openCodeTheme)?.tokens(dark)?.isDark ?: dark

/**
 * Maps an OpenCode theme onto the app tokens like the OpenCode terminal app uses them (the table
 * is in docs/design-system.md): the page is `background.base`, panels and the user message
 * `background.raised.*`, the interactive hue the primary color, `text.feedback.*` the states.
 */
fun OpenCodeThemeTokens.toOcColors(): OcColors {
    fun state(fg: Color) = OcStateColors(
        fg = fg,
        bg = fg.copy(alpha = STATE_BACKGROUND_ALPHA).compositeOver(background),
        border = fg.copy(alpha = STATE_BORDER_ALPHA).compositeOver(background),
    )
    val agent = { index: Int -> if (categorical.isEmpty()) primary else categorical[index % categorical.size] }
    return OcColors(
        isDark = isDark,
        bgDeep = background,
        bgBase = background,
        layer01 = raised,
        layer02 = raisedHigh,
        layer03 = raisedMax,
        // Skeletons and the strongest fills: between the highest panel and muted text.
        layer04 = lerp(raisedMax, textMuted, 0.5f),
        bgInverse = text,
        bgAccent = primary,
        onAccent = actionPrimaryFocusedText,
        textBase = text,
        textMuted = textMuted,
        textFaint = textFaint,
        textAccent = primary,
        textInverse = background,
        textCodePath = markdownCode,
        bgCodePath = raised,
        borderBase = border,
        borderMuted = raisedHigh,
        borderStrong = scrollbar,
        borderFocus = primary,
        iconBase = text,
        iconMuted = textMuted,
        userBubbleBg = raised,
        userBubbleText = text,
        scrim = if (isDark) OcDarkColors.scrim else OcLightColors.scrim,
        success = state(success),
        warning = state(warning),
        danger = state(error),
        info = state(info),
        agents = OcAgentColors(build = agent(0), plan = agent(1), explore = agent(2), review = agent(3), writer = agent(4)),
        diff = OcDiffColors(
            added = diffAdded,
            removed = diffRemoved,
            text = text,
            hunkText = diffHunkHeader,
            addedBackground = diffAddedBackground,
            removedBackground = diffRemovedBackground,
            contextBackground = diffContextBackground,
            hunkBackground = raisedHigh,
            lineNumber = diffLineNumber,
            lineNumberAddedBackground = diffLineNumberAddedBackground,
            lineNumberRemovedBackground = diffLineNumberRemovedBackground,
        ),
        markdown = markdownPalette(),
        syntax = OcSyntaxColors(
            comment = syntaxComment,
            keyword = syntaxKeyword,
            function = syntaxFunction,
            variable = syntaxVariable,
            string = syntaxString,
            number = syntaxNumber,
            type = syntaxType,
            operator = syntaxOperator,
            punctuation = syntaxPunctuation,
        ),
        ansi = terminalAnsi(),
    )
}

/** The markdown tokens of the theme. */
fun OpenCodeThemeTokens.markdownPalette(): OcMarkdownPalette = OcMarkdownPalette(
    text = markdownText,
    heading = markdownHeading,
    strong = markdownStrong,
    emphasis = markdownEmphasis,
    inlineCode = markdownCode,
    linkText = markdownLinkText,
    blockQuote = markdownBlockQuote,
    listBullet = markdownListItem,
    listNumber = markdownListEnumeration,
    codeBlock = markdownCodeBlock,
    horizontalRule = markdownHorizontalRule,
)

/**
 * The 16 ANSI colors from the theme's hues: colors at step 200, bright ones at step 100. Black and
 * white come from the neutral hue so that black stays dark and white light on either background:
 * dark themes use neutral 900 / 500 for black / bright black and 200 / 100 for white / bright white;
 * light themes 100 / 300 for black and 400 / 500 for white, readable on a light page.
 */
fun OpenCodeThemeTokens.terminalAnsi(): List<Color> {
    val black = if (isDark) neutral900 else neutral100
    val brightBlack = if (isDark) neutral500 else neutral300
    val white = if (isDark) neutral200 else neutral400
    val brightWhite = if (isDark) neutral100 else neutral500
    return listOf(
        black, red, green, yellow, blue, purple, cyan, white,
        brightBlack, redBright, greenBright, yellowBright, blueBright, purpleBright, cyanBright, brightWhite,
    )
}
