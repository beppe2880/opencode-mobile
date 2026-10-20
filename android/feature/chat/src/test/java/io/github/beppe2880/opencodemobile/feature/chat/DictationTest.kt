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
}
