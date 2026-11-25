package io.github.beppe2880.opencodemobile.feature.sessions

import io.github.beppe2880.opencodemobile.core.ui.sample.SampleData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BulkMuteActionTest {

    private fun session(id: String) = SampleData.sessions.first().copy(id = id)
    private val a = session("a")
    private val b = session("b")

    @Test
    fun `all muted offers unmute`() {
        assertEquals(listOf(BulkMuteAction.Unmute), bulkMuteActions(listOf(a, b), setOf("a", "b")))
    }

    @Test
    fun `all unmuted offers mute`() {
        assertEquals(listOf(BulkMuteAction.Mute), bulkMuteActions(listOf(a, b), emptySet()))
    }

    @Test
    fun `mixed offers both`() {
        assertEquals(listOf(BulkMuteAction.Mute, BulkMuteAction.Unmute), bulkMuteActions(listOf(a, b), setOf("a")))
    }

    @Test
    fun `an empty selection offers nothing`() {
        assertTrue(bulkMuteActions(emptyList(), emptySet()).isEmpty())
    }
}
