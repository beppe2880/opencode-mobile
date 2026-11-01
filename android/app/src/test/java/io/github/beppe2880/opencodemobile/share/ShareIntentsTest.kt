package io.github.beppe2880.opencodemobile.share

import io.github.beppe2880.opencodemobile.core.domain.MAX_SHARED_IMAGES
import io.github.beppe2880.opencodemobile.core.domain.MAX_SHARED_TEXT
import io.github.beppe2880.opencodemobile.share.ShareIntents.ACTION_SEND
import io.github.beppe2880.opencodemobile.share.ShareIntents.ACTION_SEND_MULTIPLE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareIntentsTest {

    private val own = "io.github.beppe2880.opencodemobile.debug"

    private fun parse(
        action: String? = ACTION_SEND,
        type: String? = "text/plain",
        text: CharSequence? = null,
        subject: CharSequence? = null,
        streams: List<String?> = emptyList(),
    ) = ShareIntents.parse(action, type, text, subject, streams, own)

    private fun image(n: Int) = "content://com.google.android.apps.photos.contentprovider/0/1/media%2F$n/ORIGINAL"

    @Test
    fun `plain text is shared as is, trimmed`() {
        val request = parse(text = "  java.lang.IllegalStateException: boom\n\tat Main.kt:12\n")!!
        assertEquals("java.lang.IllegalStateException: boom\n\tat Main.kt:12", request.text)
        assertEquals(emptyList<String>(), request.images)
        assertFalse(request.truncated)
    }

    @Test
    fun `a link comes with the page title above it`() {
        val request = parse(text = "https://github.com/o/r/issues/1", subject = "Crash on start · Issue #1")!!
        assertEquals("Crash on start · Issue #1\nhttps://github.com/o/r/issues/1", request.text)
        // A subject already in the text is not repeated, a missing text leaves the subject.
        assertEquals("Title https://x.y", parse(text = "Title https://x.y", subject = "Title")!!.text)
        assertEquals("Only a subject", parse(subject = "Only a subject")!!.text)
    }

    @Test
    fun `control characters are removed and line breaks unified`() {
        assertEquals("a\nb\nc", parse(text = "a\r\nb\rc\u0000\u001b")!!.text)
    }

    @Test
    fun `long text is cut`() {
        val request = parse(text = "x".repeat(MAX_SHARED_TEXT * 3))!!
        assertEquals(MAX_SHARED_TEXT, request.text.length)
        assertTrue(request.truncated)
    }

    @Test
    fun `one image, with its caption`() {
        val request = parse(type = "image/png", text = "Look at this", streams = listOf(image(1)))!!
        assertEquals(listOf(image(1)), request.images)
        assertEquals("Look at this", request.text)
        assertEquals(0, request.rejected)
    }

    @Test
    fun `several images, at most the limit`() {
        val uris = (1..MAX_SHARED_IMAGES + 2).map(::image)
        val request = parse(action = ACTION_SEND_MULTIPLE, type = "image/*", streams = uris)!!
        assertEquals(uris.take(MAX_SHARED_IMAGES), request.images)
        assertEquals(2, request.rejected)
    }

    @Test
    fun `file URIs, this app's providers and missing streams are refused`() {
        val request = parse(
            action = ACTION_SEND_MULTIPLE,
            type = "image/jpeg",
            streams = listOf(
                "file:///data/data/$own/shared_prefs/settings.xml",
                "content://$own.androidx-startup/x",
                "content://$own/../x",
                null,
                image(1),
            ),
        )!!
        assertEquals(listOf(image(1)), request.images)
        assertEquals(4, request.rejected)
        // Nothing usable left.
        assertNull(parse(type = "image/png", streams = listOf("file:///sdcard/a.png")))
    }

    @Test
    fun `other types and actions are not accepted`() {
        assertNull(parse(type = "application/pdf", streams = listOf(image(1))))
        assertNull(parse(type = "text/html", text = "<b>x</b>"))
        assertNull(parse(type = null, text = "x"))
        assertNull(parse(action = "android.intent.action.VIEW", text = "x"))
        // Several texts are not a share this app takes.
        assertNull(parse(action = ACTION_SEND_MULTIPLE, type = "text/plain", text = "x"))
        // Parameters and case of the type do not matter.
        assertEquals("x", parse(type = "Text/Plain; charset=UTF-8", text = "x")!!.text)
    }

    @Test
    fun `a text share's stream is not read and empty shares are refused`() {
        assertNull(parse(text = "   ", streams = listOf(image(1))))
        assertNull(parse(text = null))
        assertNull(parse(action = ACTION_SEND_MULTIPLE, type = "image/*", streams = emptyList()))
    }
}
