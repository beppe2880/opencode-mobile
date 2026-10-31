package io.github.beppe2880.opencodemobile.feature.settings.update

import io.github.beppe2880.opencodemobile.core.domain.AppRelease
import kotlin.test.Test
import kotlin.test.assertEquals

class UpdateTargetTest {

    private val release = AppRelease("1.0.0", "https://github.com/beppe2880/opencode-mobile/releases/tag/v1.0.0")

    @Test
    fun androidDownloadsFromTheGitHubRelease() {
        assertEquals(UpdateAction(UpdateActionKind.Download, release.url), UpdateTarget.Android.actionFor(release))
    }

    @Test
    fun iosOnlyShowsTheReleaseNotesWithoutAStorePage() {
        assertEquals(UpdateAction(UpdateActionKind.ReleaseNotes, release.url), UpdateTarget.Ios.actionFor(release))
    }

    @Test
    fun aStorePageWinsOverTheRelease() {
        val store = "https://apps.apple.com/app/id1"
        assertEquals(UpdateAction(UpdateActionKind.Store, store), UpdateTarget.Ios.copy(storeUrl = store).actionFor(release))
        assertEquals(UpdateAction(UpdateActionKind.Store, store), UpdateTarget.Android.copy(storeUrl = store).actionFor(release))
    }
}
