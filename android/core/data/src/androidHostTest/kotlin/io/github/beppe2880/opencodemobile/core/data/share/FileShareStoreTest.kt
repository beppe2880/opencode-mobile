package io.github.beppe2880.opencodemobile.core.data.share

import io.github.beppe2880.opencodemobile.core.domain.PendingShare
import io.github.beppe2880.opencodemobile.core.domain.PromptAttachment
import io.github.beppe2880.opencodemobile.core.domain.SharedContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileShareStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val share = PendingShare(
        id = 1,
        content = SharedContent(
            text = "Crash on start\nat Main.kt:12",
            attachments = listOf(
                PromptAttachment("data:image/png;base64,AAAA", "a.png", "image/png"),
                PromptAttachment("data:image/jpeg;base64,BBBB", "b.jpg", "image/jpeg"),
            ),
        ),
    )

    @Test
    fun `a share is written, read back, replaced and deleted`() {
        val dir = File(folder.root, "share")
        val store = FileShareStore(dir)
        assertNull(store.read())

        store.write(share)
        assertEquals(share, FileShareStore(dir).read())

        val smaller = share.copy(id = 2, content = SharedContent(text = "only text"), sessionId = "ses_1", serverId = "s1")
        store.write(smaller)
        assertEquals(smaller, store.read())
        // No image of the previous share is left behind.
        assertEquals(listOf("share.json"), dir.list()!!.toList())

        store.write(null)
        assertFalse(dir.exists())
        assertNull(store.read())
    }

    @Test
    fun `a damaged share is dropped`() {
        val dir = File(folder.root, "share")
        val store = FileShareStore(dir)
        store.write(share)
        File(dir, "attachment-1").delete()

        assertNull(store.read())
        assertFalse(dir.exists())
    }
}
