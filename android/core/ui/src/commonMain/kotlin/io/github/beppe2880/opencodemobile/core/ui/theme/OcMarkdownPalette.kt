package io.github.beppe2880.opencodemobile.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The markdown colors of an OpenCode theme ([OcColors.markdown]), styled like the markdown scopes
 * of the OpenCode terminal app's syntax rules:
 *
 * - [text]: `markdown.text`, the paragraphs.
 * - [heading]: `markup.heading` = `markdown.heading`, bold; `markup.heading.1` is also underlined.
 * - [strong]: `markup.strong` = `markdown.strong`, bold.
 * - [emphasis]: `markup.italic` = `markdown.emphasis`, italic.
 * - [inlineCode]: `markup.raw.inline` = `markdown.code`.
 * - [linkText]: `markup.link.label`, the visible text of a link = `markdown.linkText`, underlined.
 *   The URL (`markup.link.url` = `markdown.link`) is never shown, so it has no color here.
 * - [blockQuote]: `markup.quote` = `markdown.blockQuote`, italic, for the quote text and its bar.
 * - [listBullet]: `markup.list` bullets = `markdown.listItem`.
 * - [listNumber]: ordered list numbers = `markdown.listEnumeration`.
 * - [codeBlock]: the text of code blocks = `markdown.codeBlock` (syntax colors come on top).
 * - [horizontalRule]: `markdown.horizontalRule`, also the table lines.
 */
@Immutable
data class OcMarkdownPalette(
    val text: Color,
    val heading: Color,
    val strong: Color,
    val emphasis: Color,
    val inlineCode: Color,
    val linkText: Color,
    val blockQuote: Color,
    val listBullet: Color,
    val listNumber: Color,
    val codeBlock: Color,
    val horizontalRule: Color,
)
