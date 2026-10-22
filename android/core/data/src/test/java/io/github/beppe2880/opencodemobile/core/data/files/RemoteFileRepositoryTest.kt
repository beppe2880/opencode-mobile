package io.github.beppe2880.opencodemobile.core.data.files

import io.github.beppe2880.opencodemobile.core.domain.FileContent
import io.github.beppe2880.opencodemobile.core.network.FileBytes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteFileRepositoryTest {

    @Test
    fun `paths are read relative to the session folder, or from their own folder outside it`() {
        assertEquals("/home/me/app" to "src/Main.kt", fileLocation("/home/me/app", "/home/me/app/src/Main.kt"))
        assertEquals("/home/me/app" to "src/Main.kt", fileLocation("/home/me/app/", "src/Main.kt"))
        assertEquals("/home/me/app" to "src/Main.kt", fileLocation("/home/me/app", "./src/Main.kt"))
        assertEquals("/tmp/logs" to "out.txt", fileLocation("/home/me/app", "/tmp/logs/out.txt"))
        // A sibling folder that only shares the prefix is outside.
        assertEquals("/home/me/app2" to "x.kt", fileLocation("/home/me/app", "/home/me/app2/x.kt"))
        assertEquals("/" to "etc", fileLocation("/home/me/app", "/etc"))
    }

    @Test
    fun `text keeps its content and reports a cut read`() {
        val whole = fileContent(FileBytes("fun main() {}\n".encodeToByteArray(), "text/x-kotlin", 14))
        assertEquals(FileContent.Text("fun main() {}\n", 14, truncated = false), whole)

        // "é" is two bytes: the read stops inside it.
        val bytes = "caffè".encodeToByteArray()
        val cut = fileContent(FileBytes(bytes.copyOf(bytes.size - 1), "text/plain", 100)) as FileContent.Text
        assertEquals("caff", cut.text)
        assertTrue(cut.truncated)
    }

    @Test
    fun `images are shown whole and other binaries are not shown`() {
        val png = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 0, 0)
        assertTrue(fileContent(FileBytes(png, "image/png", 4)) is FileContent.Image)
        assertEquals(FileContent.Binary("image/png", 400), fileContent(FileBytes(png, "image/png", 400)))
        assertEquals(FileContent.Binary("application/octet-stream", 4), fileContent(FileBytes(png, null, 4)))
        // SVG is markup: shown as text.
        assertTrue(fileContent(FileBytes("<svg/>".encodeToByteArray(), "image/svg+xml", 6)) is FileContent.Text)
    }
}
