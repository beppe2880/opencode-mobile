package io.github.beppe2880.opencodemobile.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WhatsNewTest {

    private fun release(version: String) = AppRelease(version, "https://github.com/beppe2880/opencode-mobile/releases/tag/v$version")

    @Test
    fun theLatestVersionIsTheHighestWhateverTheOrder() {
        assertEquals("0.10.0", WhatsNew.latestVersion(listOf(release("0.9.0"), release("0.10.0"), release("0.2.0"))))
        assertEquals("1.0.0", WhatsNew.latestVersion(listOf(release("nightly"), release("1.0.0"))))
        assertNull(WhatsNew.latestVersion(emptyList()))
        assertNull(WhatsNew.latestVersion(listOf(release("nightly"))))
    }

    @Test
    fun theDotShowsUntilTheNewestReleaseWasSeen() {
        // Never opened: every release is new.
        assertTrue(WhatsNew.hasUnseen(latest = "1.0.0", seen = null))
        assertFalse(WhatsNew.hasUnseen(latest = "1.0.0", seen = "1.0.0"))
        assertTrue(WhatsNew.hasUnseen(latest = "1.0.1", seen = "1.0.0"))
        // Numbers are compared, not text.
        assertTrue(WhatsNew.hasUnseen(latest = "0.10.0", seen = "0.9.0"))
        assertFalse(WhatsNew.hasUnseen(latest = "0.9.0", seen = "0.10.0"))
    }

    @Test
    fun noDotWithoutAKnownRelease() {
        assertFalse(WhatsNew.hasUnseen(latest = null, seen = null))
        assertFalse(WhatsNew.hasUnseen(latest = null, seen = "1.0.0"))
        assertFalse(WhatsNew.hasUnseen(latest = "nightly", seen = null))
    }

    @Test
    fun theButtonIsShownUntilItIsHidden() {
        assertTrue(WhatsNew.buttonVisible(latest = null, hidden = null))
        assertTrue(WhatsNew.buttonVisible(latest = "1.0.0", hidden = null))
        assertFalse(WhatsNew.buttonVisible(latest = "1.0.0", hidden = "1.0.0"))
    }

    @Test
    fun aHiddenButtonComesBackWithANewerRelease() {
        assertFalse(WhatsNew.buttonVisible(latest = "1.0.0", hidden = "1.0.0"))
        assertTrue(WhatsNew.buttonVisible(latest = "1.0.1", hidden = "1.0.0"))
        assertTrue(WhatsNew.buttonVisible(latest = "0.10.0", hidden = "0.9.0"))
        // Fewer releases known than when it was hidden (an older cache): still hidden.
        assertFalse(WhatsNew.buttonVisible(latest = "0.9.0", hidden = "1.0.0"))
    }

    @Test
    fun hiddenWithoutAKnownReleaseItWaitsForOne() {
        // Hidden offline on 0.9.0: nothing is known yet.
        assertFalse(WhatsNew.buttonVisible(latest = null, hidden = "0.9.0"))
        assertFalse(WhatsNew.buttonVisible(latest = "0.9.0", hidden = "0.9.0"))
        assertTrue(WhatsNew.buttonVisible(latest = "0.9.1", hidden = "0.9.0"))
        // Hidden on a build without a version: the first release found brings it back.
        assertFalse(WhatsNew.buttonVisible(latest = null, hidden = "dev"))
        assertTrue(WhatsNew.buttonVisible(latest = "0.9.0", hidden = "dev"))
    }

    @Test
    fun theHistoryDecidesFromItsReleases() {
        val history = WhatsNewHistory(releases = listOf(release("1.1.0"), release("1.0.0")), seenVersion = "1.0.0", hiddenVersion = "1.0.0")
        assertEquals("1.1.0", history.latestVersion)
        assertTrue(history.hasUnseen)
        assertTrue(history.buttonVisible)

        val settled = history.copy(seenVersion = "1.1.0", hiddenVersion = "1.1.0")
        assertFalse(settled.hasUnseen)
        assertFalse(settled.buttonVisible)

        val fresh = WhatsNewHistory()
        assertFalse(fresh.hasUnseen)
        assertTrue(fresh.buttonVisible)
    }
}
