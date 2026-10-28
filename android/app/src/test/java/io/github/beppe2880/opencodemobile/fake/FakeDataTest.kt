package io.github.beppe2880.opencodemobile.fake

import io.github.beppe2880.opencodemobile.core.domain.Message
import io.github.beppe2880.opencodemobile.core.domain.PromptDelivery
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeDataTest {
    @Test
    fun `a new demo restores sample data without retaining edits`() = runTest {
        val first = FakeSessions(FakeStore(), FakeEvents())
        val sample = first.list(null, null, 100).getOrThrow().first()
        first.rename(sample.id, "Changed in demo").getOrThrow()
        first.delete(sample.id).getOrThrow()

        val fresh = FakeSessions(FakeStore(), FakeEvents())
        assertEquals(sample.title, fresh.get(sample.id).getOrThrow().title)
    }

    @Test
    fun `a prompt produces a scripted transcript without a server`() = runTest {
        val store = FakeStore()
        val events = FakeEvents()
        val sessions = FakeSessions(store, events)
        val chat = FakeChat(store, events, backgroundScope)
        val session = sessions.create("/demo", "build", null).getOrThrow()
        chat.prompt(session.id, "Hello demo", PromptDelivery.Steer, emptyList()).getOrThrow()
        // Reading after virtual time advances also verifies the completed reply was stored.
        testScheduler.advanceTimeBy(15_000)
        val messages = chat.messages(session.id).getOrThrow()
        assertTrue(messages.any { it is Message.User && it.text == "Hello demo" })
        assertTrue(messages.any { it is Message.Assistant })
        assertFalse(session.id in store.running)
    }
}
