package io.github.beppe2880.opencodemobile.core.data.share

import io.github.beppe2880.opencodemobile.core.domain.PendingShare
import io.github.beppe2880.opencodemobile.core.domain.PromptAttachment
import io.github.beppe2880.opencodemobile.core.domain.SharedContent
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StoredShareRepositoryTest {

    private val image = PromptAttachment(uri = "data:image/png;base64,AAAA", name = "shot.png", mime = "image/png")
    private val content = SharedContent(text = "https://github.com/o/r/issues/1", attachments = listOf(image), skipped = 1)
    private var now = 100L

    private fun TestScope.repository(store: ShareStore) =
        StoredShareRepository(store, backgroundScope, io = StandardTestDispatcher(testScheduler), clock = { now++ })

    @Test
    fun aShareIsKeptUntilItsChatTakesIt() = runTest {
        val store = MemoryShareStore()
        val shares = repository(store)
        shares.offer(content)
        assertEquals(content, shares.pending.value?.content)
        assertTrue(shares.pending.value!!.needsChatOn("s1"))

        // Not assigned yet: no chat takes it.
        assertNull(shares.take("ses_1"))
        shares.assign("ses_1", "s1")
        assertFalse(shares.pending.value!!.needsChatOn("s1"))
        // Another server still has to place it.
        assertTrue(shares.pending.value!!.needsChatOn("s2"))
        assertNull(shares.take("ses_2"))

        assertEquals(content, shares.take("ses_1"))
        assertNull(shares.pending.value)
        assertNull(store.read())
        assertNull(shares.take("ses_1"))
    }

    @Test
    fun aShareSurvivesARestartAndANewOneReplacesIt() = runTest {
        val store = MemoryShareStore()
        repository(store).offer(content)
        val first = store.read()!!

        val restarted = repository(store)
        runCurrent()
        assertEquals(first, restarted.pending.value)

        restarted.offer(SharedContent(text = "newer"))
        assertEquals("newer", restarted.pending.value?.content?.text)
        assertTrue(restarted.pending.value!!.id != first.id)
        assertEquals(restarted.pending.value, store.read())
    }

    @Test
    fun discardingForgetsTheShare() = runTest {
        val store = MemoryShareStore()
        val shares = repository(store)
        shares.offer(content)
        shares.discard()
        assertNull(shares.pending.value)
        assertNull(store.read())
    }

    @Test
    fun theStoredFormKeepsEverything() {
        val share = PendingShare(id = 7, content = content.copy(truncated = true), sessionId = "ses_1", serverId = "s1")
        val (description, uris) = ShareCodec.encode(share)
        assertEquals(listOf(image.uri), uris)
        // The images are stored apart, not in the description.
        assertFalse(image.uri in description)
        assertEquals(share, ShareCodec.decode(description, uris))
    }

    @Test
    fun aDamagedStoredShareReadsAsNone() {
        val (description, uris) = ShareCodec.encode(PendingShare(id = 1, content = content))
        assertNull(ShareCodec.decode("{not json", uris))
        assertNull(ShareCodec.decode("[]", uris))
        // An image file is missing.
        assertNull(ShareCodec.decode(description, emptyList()))
    }
}
