package io.github.beppe2880.opencodemobile.feature.chat

import org.junit.Assert.assertEquals
import org.junit.Test

class DictationTest {

    @Test
    fun `dictated words follow what was typed`() {
        assertEquals("fix the build", appendDictated("", "fix the build"))
        assertEquals("fix the build", appendDictated("  ", "fix the build"))
        assertEquals("In app.ts fix the build", appendDictated("In app.ts ", "fix the build"))
        assertEquals("@README.md explain it", appendDictated("@README.md", "explain it"))
    }

    @Test
    fun `a transcript goes in at the cursor with spaces where words would touch`() {
        assertEquals("fix the build" to 13, insertSpoken("", 0, 0, " fix the build "))
        assertEquals("In app.ts fix the build" to 23, insertSpoken("In app.ts", 9, 9, "fix the build"))
        assertEquals("In app.ts fix the build" to 23, insertSpoken("In app.ts ", 10, 10, "fix the build"))
        // In the middle: the cursor lands after the inserted words and their space.
        assertEquals("Fix the login page then deploy" to 19, insertSpoken("Fix then deploy", 4, 4, "the login page"))
        // A selection is replaced.
        assertEquals("Fix the login page" to 13, insertSpoken("Fix the signup page", 8, 14, "login"))
        // Out-of-range cursors are clamped.
        assertEquals("ab cd" to 5, insertSpoken("ab", 9, 3, "cd"))
    }

    @Test
    fun `recording time reads as minutes and seconds`() {
        assertEquals("0:00", formatElapsed(0))
        assertEquals("0:07", formatElapsed(7_900))
        assertEquals("1:05", formatElapsed(65_000))
        assertEquals("5:00", formatElapsed(300_000))
    }
}
