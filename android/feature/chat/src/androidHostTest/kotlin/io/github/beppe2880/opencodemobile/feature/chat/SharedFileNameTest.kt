package io.github.beppe2880.opencodemobile.feature.chat

import org.junit.Assert.assertEquals
import org.junit.Test

class SharedFileNameTest {

    @Test
    fun `names from other apps become plain bounded file names`() {
        assertEquals("shot.png", sharedFileName("shot.png"))
        assertEquals("passwd", sharedFileName("../../etc/passwd"))
        assertEquals("evil.png", sharedFileName("C:\\Users\\x\\evil.png"))
        assertEquals("ab.png", sharedFileName("a\u0000b\n.png"))
        assertEquals(120, sharedFileName("x".repeat(500)).length)
        assertEquals("image", sharedFileName(" / "))
    }
}
