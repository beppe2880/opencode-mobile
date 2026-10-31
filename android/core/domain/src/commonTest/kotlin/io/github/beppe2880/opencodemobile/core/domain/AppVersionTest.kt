package io.github.beppe2880.opencodemobile.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppVersionTest {

    @Test
    fun parsesTagsAndVersionNames() {
        assertEquals(AppVersion.parse("0.9.0"), AppVersion.parse("v0.9.0"))
        assertEquals(AppVersion.parse("1.2"), AppVersion.parse("1.2.0"))
        assertTrue(AppVersion.parse("0.9.0-kmp")!!.isPreRelease)
        assertFalse(AppVersion.parse("1.0.0+build.5")!!.isPreRelease)
        assertNull(AppVersion.parse("dev"))
        assertNull(AppVersion.parse(""))
        assertNull(AppVersion.parse("1.0.0 beta"))
        assertNull(AppVersion.parse("nightly-1.0"))
    }

    @Test
    fun comparesNumericallyPartByPart() {
        assertTrue(AppVersion.parse("0.10.0")!! > AppVersion.parse("0.9.9")!!)
        assertTrue(AppVersion.parse("1.0")!! > AppVersion.parse("0.99.99")!!)
        assertTrue(AppVersion.parse("0.9.1")!! > AppVersion.parse("0.9")!!)
    }

    @Test
    fun onlyNewerStableReleasesAreUpdates() {
        assertTrue(AppVersion.isUpdate("v0.10.0", "0.9.0"))
        assertTrue(AppVersion.isUpdate("v1.0.0", "0.9.0-kmp"))
        assertFalse(AppVersion.isUpdate("v0.9.0", "0.9.0"))
        assertFalse(AppVersion.isUpdate("v0.8.0", "0.9.0"))
        // A local build with a suffix is not told that its own version is newer.
        assertFalse(AppVersion.isUpdate("v0.9.0", "0.9.0-kmp"))
        assertFalse(AppVersion.isUpdate("v1.0.0-beta.1", "0.9.0"))
        assertTrue(AppVersion.isUpdate("v1.0.0+build.2", "0.9.0"))
        // Unknown versions are never nagged.
        assertFalse(AppVersion.isUpdate("v1.0.0", "dev"))
        assertFalse(AppVersion.isUpdate("latest", "0.9.0"))
    }
}
