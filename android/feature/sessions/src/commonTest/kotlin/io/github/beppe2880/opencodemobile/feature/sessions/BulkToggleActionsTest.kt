package io.github.beppe2880.opencodemobile.feature.sessions

import io.github.beppe2880.opencodemobile.core.ui.sample.SampleData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BulkToggleActionsTest {

    private fun session(id: String, archived: Boolean = false) = SampleData.sessions.first().copy(
        id = id,
        archivedAt = if (archived) 1L else null,
    )
    private val active = session("active")
    private val archived = session("archived", archived = true)

    @Test
    fun `all pinned offers unpin`() {
        assertEquals(listOf(BulkPinAction.Unpin), bulkPinActions(listOf(active, archived), setOf("active", "archived")))
    }

    @Test
    fun `all unpinned offers pin`() {
        assertEquals(listOf(BulkPinAction.Pin), bulkPinActions(listOf(active, archived), emptySet()))
    }

    @Test
    fun `mixed pins offer both`() {
        assertEquals(listOf(BulkPinAction.Pin, BulkPinAction.Unpin), bulkPinActions(listOf(active, archived), setOf("active")))
    }

    @Test
    fun `all archived offers unarchive`() {
        assertEquals(
            listOf(BulkArchiveAction.Unarchive),
            bulkArchiveActions(listOf(archived, archived.copy(id = "archived-2"))),
        )
    }

    @Test
    fun `all active offers archive`() {
        assertEquals(
            listOf(BulkArchiveAction.Archive),
            bulkArchiveActions(listOf(active, active.copy(id = "active-2"))),
        )
    }

    @Test
    fun `mixed archived offers both`() {
        assertEquals(listOf(BulkArchiveAction.Archive, BulkArchiveAction.Unarchive), bulkArchiveActions(listOf(active, archived)))
    }

    @Test
    fun `an empty selection offers nothing`() {
        assertTrue(bulkPinActions(emptyList(), emptySet()).isEmpty())
        assertTrue(bulkArchiveActions(emptyList()).isEmpty())
    }
}
