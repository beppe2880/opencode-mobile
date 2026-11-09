package io.github.beppe2880.opencodemobile.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenCodeThemesTest {

    @Test
    fun `every TUI theme is generated with the default one first`() {
        val ids = OpenCodeThemes.all.map { it.id }
        assertEquals(33, ids.size)
        assertEquals("opencode", ids.first())
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(ids.drop(1).sorted(), ids.drop(1))
        assertTrue("catppuccin-frappe" in ids && "tokyonight" in ids)
        OpenCodeThemes.all.forEach { assertEquals(it.id, it.displayName) }
    }

    @Test
    fun `byId finds a theme and null or an unknown id is none`() {
        assertEquals("nord", OpenCodeThemes.byId("nord")?.id)
        assertNull(OpenCodeThemes.byId(null))
        assertNull(OpenCodeThemes.byId("no-such-theme"))
    }

    @Test
    fun `no theme keeps the app colors`() {
        assertSame(OcLightColors, ocColors(null, dark = false))
        assertSame(OcDarkColors, ocColors(null, dark = true))
        assertSame(OcDarkColors, ocColors("no-such-theme", dark = true))
        assertTrue(isDarkTheme(null, dark = true))
        assertFalse(isDarkTheme(null, dark = false))
    }

    @Test
    fun `the default dark theme maps like the TUI`() {
        val c = ocColors("opencode", dark = true)
        assertTrue(c.isDark)
        assertEquals(Color(0xFF0A0A0A), c.bgBase)
        assertEquals(Color(0xFF0A0A0A), c.bgDeep)
        assertEquals(Color(0xFF141414), c.layer01)
        assertEquals(Color(0xFF1E1E1E), c.layer02)
        assertEquals(Color(0xFF4C4C4C), c.layer03)
        assertEquals(Color(0xFFEEEEEE), c.textBase)
        assertEquals(Color(0xFF808080), c.textMuted)
        // Primary is the interactive hue (orange in dark), with the TUI's focused text on it.
        assertEquals(Color(0xFFFAB283), c.bgAccent)
        assertEquals(Color(0xFFFAB283), c.textAccent)
        assertEquals(Color(0xFF0A0A0A), c.onAccent)
        assertEquals(Color(0xFF484848), c.borderBase)
        assertEquals(Color(0xFF606060), c.borderStrong)
        assertEquals(Color(0xFF7FD88F), c.textCodePath)
        assertEquals(Color(0xFFE06C75), c.danger.fg)
        assertEquals(Color(0xFFE06C75).copy(alpha = 0.15f).compositeOver(c.bgBase), c.danger.bg)
        assertEquals(Color(0xFFF5A742), c.warning.fg)
        // Agents: the categorical hues at step 200 in order (blue, purple, green, orange, red).
        assertEquals(Color(0xFF5C9CF5), c.agents.build)
        assertEquals(Color(0xFF9D7CD8), c.agents.plan)
        assertEquals(Color(0xFF7FD88F), c.agents.explore)
        assertEquals(Color(0xFFE06C75), c.agents.writer)
        assertEquals(Color(0xFF4FD6BE), c.diff.added)
        assertEquals(Color(0xFF20303B), c.diff.addedBackground)
    }

    @Test
    fun `the default light theme is light`() {
        val c = ocColors("opencode", dark = false)
        assertFalse(c.isDark)
        assertEquals(Color(0xFFFFFFFF), c.bgBase)
        assertEquals(Color(0xFF1A1A1A), c.textBase)
        assertEquals(Color(0xFF3B7DD8), c.bgAccent)
    }

    @Test
    fun `a theme with one mode uses it for both and keeps its own darkness`() {
        val single = OpenCodeThemes.all.filter { it.light == it.dark }
        for (theme in single) {
            val requestedLight = ocColors(theme.id, dark = false)
            assertEquals(theme.dark.isDark, requestedLight.isDark)
            assertEquals(theme.dark.isDark, isDarkTheme(theme.id, dark = false))
            assertEquals(theme.dark.isDark, isDarkTheme(theme.id, dark = true))
        }
    }

    @Test
    fun `every theme maps to a complete color set`() {
        for (theme in OpenCodeThemes.all) {
            for (dark in listOf(false, true)) {
                val tokens = theme.tokens(dark)
                val c = tokens.toOcColors()
                assertEquals(tokens.isDark, c.isDark)
                assertEquals(16, c.terminalAnsi.size)
                assertTrue(tokens.categorical.isNotEmpty())
                val preview = theme.preview(dark)
                assertEquals(tokens.background, preview.background)
                assertEquals(tokens.markdownStrong, preview.strong)
            }
        }
    }

    @Test
    fun `the terminal takes the theme hues`() {
        val ansi = ocColors("opencode", dark = true).terminalAnsi
        assertEquals(Color(0xFFE06C75), ansi[1])
        assertEquals(Color(0xFF7FD88F), ansi[2])
        // The default theme has no yellow hue: yellow is its orange.
        assertEquals(Color(0xFFFAB283), ansi[3])
        assertEquals(Color(0xFF5C9CF5), ansi[4])
        assertEquals(Color(0xFF9D7CD8), ansi[5])
        assertEquals(Color(0xFF56B6C2), ansi[6])
        assertEquals(Color(0xFFEEEEEE), ansi[7])
        assertEquals(Color(0xFFFD7E87), ansi[9])
    }
}
