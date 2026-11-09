package io.github.beppe2880.opencodemobile.core.ui.markdown

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import io.github.beppe2880.opencodemobile.core.ui.theme.OcDarkColors
import io.github.beppe2880.opencodemobile.core.ui.theme.OcLightColors
import io.github.beppe2880.opencodemobile.core.ui.theme.OcMonoTypography
import io.github.beppe2880.opencodemobile.core.ui.theme.markdownPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MarkdownTypographyTest {

    private val material = Typography()
    private val mono = OcMonoTypography(code = TextStyle(), codeSmall = TextStyle(), label = TextStyle())

    @Test
    fun `the palettes are the TUI default theme colors`() {
        val dark = OcDarkColors.markdownPalette
        assertEquals(Color(0xFF9D7CD8), dark.heading)
        assertEquals(Color(0xFFF5A742), dark.strong)
        assertEquals(Color(0xFFE5C07B), dark.emphasis)
        assertEquals(Color(0xFF7FD88F), dark.inlineCode)
        assertEquals(Color(0xFF56B6C2), dark.linkText)
        assertEquals(Color(0xFFE5C07B), dark.blockQuote)
        assertEquals(Color(0xFFFAB283), dark.listBullet)
        assertEquals(Color(0xFF56B6C2), dark.listNumber)
        val light = OcLightColors.markdownPalette
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
    fun `plain markdown keeps the text and accent colors`() {
        val c = OcDarkColors
        val t = markdownTypography(material, mono, c, palette = null)
        assertEquals(c.textBase, t.paragraph.color)
        assertEquals(c.textBase, t.bullet.color)
        assertEquals(c.textBase, t.ordered.color)
        assertEquals(c.textMuted, t.quote.color)
        assertNull(t.quote.fontStyle)
        assertEquals(c.textAccent, t.textLink.style?.color)
    }

    @Test
    fun `colored markdown follows the TUI rules`() {
        val c = OcDarkColors
        val p = c.markdownPalette
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
        // Outside quotes the text, and code blocks, stay as they are.
        assertEquals(c.textBase, t.paragraph.color)
        assertNull(t.paragraph.fontStyle)
        assertEquals(mono.codeSmall, t.code)
    }

    @Test
    fun `the text of a colored quote is the quote style and nested styles keep their colors`() {
        val c = OcLightColors
        val p = c.markdownPalette
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
}
