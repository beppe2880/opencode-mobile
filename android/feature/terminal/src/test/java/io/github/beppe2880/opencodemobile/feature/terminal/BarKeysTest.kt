package io.github.beppe2880.opencodemobile.feature.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BarKeysTest {

    @Test
    fun `every key survives being saved and read back`() {
        val keys = BarKeys.Catalog + BarKey.Text("wq", "\u001b:wq\r") + BarKey.Text("ls", "ls -la\r")

        assertEquals(keys, keys.map { BarKey.parse(it.spec) })
    }

    @Test
    fun `keys from a newer or corrupted layout are skipped`() {
        assertNull(BarKey.parse("hyper"))
        assertNull(BarKey.parse("char:"))
        assertNull(BarKey.parse("text:no-separator"))
        assertEquals(listOf(listOf(BarKey.Special(SpecialKey.Escape)), emptyList<BarKey>()), BarKeys.fromSettings(listOf(listOf("esc", "hyper"), emptyList())))
    }

    @Test
    fun `no saved layout or a layout with another row count falls back to the default`() {
        assertEquals(BarKeys.Default, BarKeys.fromSettings(null))
        assertEquals(BarKeys.Default, BarKeys.fromSettings(listOf(listOf("esc"))))
        assertEquals(BarKeys.Default, BarKeys.fromSettings(BarKeys.toSettings(BarKeys.Default)))
    }

    @Test
    fun `custom key text understands escapes and caret notation`() {
        assertEquals("\u001b:wq\r", decodeKeyText("\\e:wq\\r"))
        assertEquals("\u0003", decodeKeyText("^C"))
        assertEquals("\u0003", decodeKeyText("^c"))
        assertEquals("a\\b\tc\n", decodeKeyText("a\\\\b\\tc\\n"))
        assertEquals("trailing\\", decodeKeyText("trailing\\"))
        assertEquals("x^", decodeKeyText("x^"))
    }

    @Test
    fun `a modifier is armed for one key, then locked, then released`() {
        assertEquals(ModifierState.Once, ModifierState.Off.next())
        assertEquals(ModifierState.Locked, ModifierState.Once.next())
        assertEquals(ModifierState.Off, ModifierState.Locked.next())
    }
}
