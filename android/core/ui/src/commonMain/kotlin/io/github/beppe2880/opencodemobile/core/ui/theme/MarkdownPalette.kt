package io.github.beppe2880.opencodemobile.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The markdown colors of the OpenCode terminal app's default theme, used by colored markdown in
 * the chat. The values are the `markdown*` tokens of `packages/tui/src/theme/assets/opencode.json`
 * and the styles follow the markdown scopes of the syntax rules in `packages/tui/src/theme/index.ts`:
 *
 * - [heading]: `markup.heading` = `markdownHeading`, bold; `markup.heading.1` is also underlined.
 * - [strong]: `markup.strong` = `markdownStrong`, bold.
 * - [emphasis]: `markup.italic` = `markdownEmph`, italic.
 * - [inlineCode]: `markup.raw.inline` = `markdownCode`.
 * - [linkText]: `markup.link.label`, the visible text of a link = `markdownLinkText`, underlined.
 *   The URL (`markup.link.url` = `markdownLink`) is never shown, so it has no color here.
 * - [blockQuote]: `markup.quote` = `markdownBlockQuote`, italic, for the quote text and its bar.
 * - [listBullet]: `markup.list` bullets = `markdownListItem`.
 * - [listNumber]: ordered list numbers = `markdownListEnumeration`.
 */
@Immutable
data class OcMarkdownPalette(
    val heading: Color,
    val strong: Color,
    val emphasis: Color,
    val inlineCode: Color,
    val linkText: Color,
    val blockQuote: Color,
    val listBullet: Color,
    val listNumber: Color,
)

/** The palette matching the light or dark app theme. */
val OcColors.markdownPalette: OcMarkdownPalette
    get() = if (isDark) DarkMarkdownPalette else LightMarkdownPalette

private val DarkMarkdownPalette = OcMarkdownPalette(
    heading = Color(0xFF9D7CD8),
    strong = Color(0xFFF5A742),
    emphasis = Color(0xFFE5C07B),
    inlineCode = Color(0xFF7FD88F),
    linkText = Color(0xFF56B6C2),
    blockQuote = Color(0xFFE5C07B),
    listBullet = Color(0xFFFAB283),
    listNumber = Color(0xFF56B6C2),
)

private val LightMarkdownPalette = OcMarkdownPalette(
    heading = Color(0xFFD68C27),
    strong = Color(0xFFD68C27),
    emphasis = Color(0xFFB0851F),
    inlineCode = Color(0xFF3D9A57),
    linkText = Color(0xFF318795),
    blockQuote = Color(0xFFB0851F),
    listBullet = Color(0xFF3B7DD8),
    listNumber = Color(0xFF318795),
)
