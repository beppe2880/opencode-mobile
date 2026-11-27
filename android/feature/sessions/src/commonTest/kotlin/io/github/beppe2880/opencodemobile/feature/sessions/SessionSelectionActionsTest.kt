package io.github.beppe2880.opencodemobile.feature.sessions

import io.github.beppe2880.opencodemobile.core.domain.SessionShortcut
import io.github.beppe2880.opencodemobile.core.domain.UnreadSessions
import io.github.beppe2880.opencodemobile.core.ui.sample.SampleData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionSelectionActionsTest {
    private val a = SampleData.sessions.first().copy(id = "a", archivedAt = null, idleAt = null)
    private val b = a.copy(id = "b", archivedAt = 1L)
    private val mixed = SessionsState(
        groups = listOf(SessionGroup(DayBucket.Today, listOf(a, b))),
        selection = setOf(a.id, b.id),
        pinned = setOf(b.id),
        muted = setOf(b.id),
        manualUnread = setOf(b.id),
    )

    @Test
    fun `mixed selections offer one direction per pair across toolbar and overflow`() {
        val available = selectionActions(mixed, muteSupported = true)
        assertEquals(
            listOf(SelectionAction.Mute, SelectionAction.MarkUnread, SelectionAction.Pin, SelectionAction.Archive, SelectionAction.Delete),
            available,
        )
        val defaults = placeSelectionActions(available, SessionShortcut.defaults, slots = 3)
        assertEquals(listOf(SelectionAction.Pin, SelectionAction.MarkUnread), defaults.toolbar)
        assertEquals(listOf(SelectionAction.Mute, SelectionAction.Archive, SelectionAction.Delete), defaults.overflow)
        assertEquals(available, placeSelectionActions(available, SessionShortcut.defaults, slots = 1).overflow)

        val wide = placeSelectionActions(available, SessionShortcut.entries, slots = 5)
        assertEquals(
            listOf(SelectionAction.Pin, SelectionAction.MarkUnread, SelectionAction.Mute, SelectionAction.Archive, SelectionAction.Delete),
            wide.toolbar,
        )
        assertTrue(wide.overflow.isEmpty())
    }

    @Test
    fun `a homogeneous selection offers only the direction that applies`() {
        val available = selectionActions(mixed.copy(selection = setOf(b.id)), muteSupported = true)

        assertEquals(
            listOf(
                SelectionAction.Rename,
                SelectionAction.Unmute,
                SelectionAction.MarkRead,
                SelectionAction.Unpin,
                SelectionAction.Unarchive,
                SelectionAction.Delete,
            ),
            available,
        )
        assertTrue(selectionActions(mixed.copy(selection = emptySet()), muteSupported = true).isEmpty())
    }

    @Test
    fun `opposite commands replace the original commands once all selected sessions change state`() {
        val homogeneous = mixed.copy(
            groups = listOf(SessionGroup(DayBucket.Today, listOf(a.copy(archivedAt = b.archivedAt), b))),
            muted = mixed.selection,
            pinned = mixed.selection,
            manualUnread = mixed.selection,
        )
        assertEquals(
            listOf(SelectionAction.Unmute, SelectionAction.MarkRead, SelectionAction.Unpin, SelectionAction.Unarchive, SelectionAction.Delete),
            selectionActions(homogeneous, muteSupported = true),
        )
    }

    @Test
    fun `unsupported actions and rename for multiple sessions do not consume a slot`() {
        val state = mixed.copy(archiveSupported = false, unreadSessions = UnreadSessions.Off)
        val available = selectionActions(state, muteSupported = false)
        val placement = placeSelectionActions(available, SessionShortcut.entries, slots = 3)

        assertEquals(listOf(SelectionAction.Pin, SelectionAction.Delete), placement.toolbar)
        assertTrue(placement.overflow.isEmpty())
    }

    @Test
    fun `overflow only disappears if all actions fit and are preferred`() {
        val available = listOf(SelectionAction.Rename, SelectionAction.Pin, SelectionAction.Delete)
        val preferences = listOf(SessionShortcut.Delete, SessionShortcut.Rename, SessionShortcut.Pin)
        val wide = placeSelectionActions(available, preferences, slots = 3)
        val narrow = placeSelectionActions(available, preferences, slots = 2)

        assertEquals(listOf(SelectionAction.Delete, SelectionAction.Rename, SelectionAction.Pin), wide.toolbar)
        assertTrue(wide.overflow.isEmpty())
        assertEquals(listOf(SelectionAction.Delete), narrow.toolbar)
        assertEquals(listOf(SelectionAction.Rename, SelectionAction.Pin), narrow.overflow)
        assertFalse(narrow.toolbar.any { it in narrow.overflow })
        assertEquals(available, placeSelectionActions(available, emptyList(), slots = 5).overflow)
        assertEquals(available, placeSelectionActions(available, preferences, slots = 0).overflow)
        assertEquals(listOf(SelectionAction.Pin), placeSelectionActions(available, SessionShortcut.defaults, slots = 5).toolbar)
    }
}
