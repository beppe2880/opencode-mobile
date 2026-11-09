package io.github.beppe2880.opencodemobile.core.ui.markdown

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import dev.snipme.highlights.model.SyntaxThemes
import io.github.beppe2880.opencodemobile.core.ui.theme.OcDarkColors
import io.github.beppe2880.opencodemobile.core.ui.theme.OcLightColors
import io.github.beppe2880.opencodemobile.core.ui.theme.OcMonoTypography
import io.github.beppe2880.opencodemobile.core.ui.theme.ocColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MarkdownTypographyTest {

    private val material = Typography()
    private val mono = OcMonoTypography(code = TextStyle(), codeSmall = TextStyle(), label = TextStyle())

    @Test
    fun `the default OpenCode theme gives the TUI markdown colors`() {
        val dark = ocColors("opencode", dark = true).markdown!!
        assertEquals(Color(0xFF9D7CD8), dark.heading)
        assertEquals(Color(0xFFF5A742), dark.strong)
        assertEquals(Color(0xFFE5C07B), dark.emphasis)
        assertEquals(Color(0xFF7FD88F), dark.inlineCode)
        assertEquals(Color(0xFF56B6C2), dark.linkText)
        assertEquals(Color(0xFFE5C07B), dark.blockQuote)
        assertEquals(Color(0xFFFAB283), dark.listBullet)
        assertEquals(Color(0xFF56B6C2), dark.listNumber)
        assertEquals(Color(0xFFEEEEEE), dark.codeBlock)
        assertEquals(Color(0xFF808080), dark.horizontalRule)
        val light = ocColors("opencode", dark = false).markdown!!
        assertEquals(Color(0xFFD68C27), light.heading)
        assertEquals(Color(0xFFD68C27), light.strong)
        assertEquals(Color(0xFFB0851F), light.emphasis)
        assertEquals(Color(0xFF3D9A57), light.inlineCode)
        assertEquals(Color(0xFF318795), light.linkText)
        assertEquals(Color(0xFFB0851F), light.blockQuote)
        assertEquals(Color(0xFF3B7DD8), light.listBullet)
        assertEquals(Color(0xFF318795), light.listNumber)
    }

    @Test
    fun `the app themes have no markdown colors`() {
        assertNull(OcLightColors.markdown)
        assertNull(OcDarkColors.markdown)
    }

    @Test
    fun `plain markdown keeps the text and accent colors`() {
        val c = OcDarkColors
        val t = markdownTypography(material, mono, c, palette = null)
        assertEquals(c.textBase, t.paragraph.color)
        assertEquals(c.textBase, t.bullet.color)
        assertEquals(c.textBase, t.ordered.color)
        assertEquals(c.textMuted, t.quote.color)
        assertNull(t.quote.fontStyle)
        assertEquals(c.textAccent, t.textLink.style?.color)
        assertEquals(mono.codeSmall, t.code)
    }

    @Test
    fun `themed markdown follows the TUI rules`() {
        val c = ocColors("opencode", dark = true)
        val p = c.markdown!!
        val t = markdownTypography(material, mono, c, p)
        assertEquals(p.heading, t.h1.color)
        assertEquals(TextDecoration.Underline, t.h1.textDecoration)
        assertEquals(p.heading, t.h2.color)
        assertNull(t.h2.textDecoration)
        assertEquals(p.inlineCode, t.inlineCode.color)
        // The link text is cyan and underlined, not the link (URL) color.
        assertEquals(p.linkText, t.textLink.style?.color)
        assertEquals(TextDecoration.Underline, t.textLink.style?.textDecoration)
        assertEquals(p.blockQuote, t.quote.color)
        assertEquals(FontStyle.Italic, t.quote.fontStyle)
        assertEquals(p.listBullet, t.bullet.color)
        assertEquals(p.listNumber, t.ordered.color)
        // Outside quotes the text is the markdown text; code blocks take the code block color.
        assertEquals(p.text, t.paragraph.color)
        assertNull(t.paragraph.fontStyle)
        assertEquals(p.codeBlock, t.code.color)
    }

    @Test
    fun `the text of a themed quote is the quote style and nested styles keep their colors`() {
        val c = ocColors("opencode", dark = false)
        val p = c.markdown!!
        val t = markdownTypography(material, mono, c, p, quoteText = true)
        for (style in listOf(t.paragraph, t.text, t.list)) {
            assertEquals(p.blockQuote, style.color)
            assertEquals(FontStyle.Italic, style.fontStyle)
        }
        assertEquals(p.inlineCode, t.inlineCode.color)
        assertEquals(p.linkText, t.textLink.style?.color)
        assertEquals(p.listBullet, t.bullet.color)
        assertEquals(p.listNumber, t.ordered.color)
    }

    @Test
    fun `code takes the theme's syntax colors and Atom One without a theme`() {
        val syntax = ocColors("opencode", dark = true).syntax!!
        val theme = syntaxTheme(syntax, dark = true)
        assertEquals(0x9D7CD8, theme.keyword)
        assertEquals(0x7FD88F, theme.string)
        assertEquals(0x808080, theme.comment)
        assertEquals(SyntaxThemes.atom(darkMode = true), syntaxTheme(null, dark = true))
    }
}
