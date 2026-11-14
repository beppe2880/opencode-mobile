package io.github.beppe2880.opencodemobile.feature.settings.whatsnew

import io.github.beppe2880.opencodemobile.core.domain.AppRelease
import io.github.beppe2880.opencodemobile.core.domain.ReleaseSectionKind
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WhatsNewTimelineTest {

    private val utc = TimeZone.UTC

    /** A time of 2026 in UTC, in epoch milliseconds. */
    private fun at(month: Int, day: Int, hour: Int = 12, minute: Int = 0): Long =
        LocalDateTime(2026, month, day, hour, minute).toInstant(TimeZone.UTC).toEpochMilliseconds()

    private fun release(version: String, publishedAt: Long?, notes: String = "### Features\n\n* Version $version") =
        AppRelease(version, "https://github.com/beppe2880/opencode-mobile/releases/tag/v$version", notes, publishedAt)

    private fun List<WhatsNewDay>.texts(type: ChangeType): List<List<String>> = map { day ->
        day.groups.firstOrNull { it.type == type }?.changes.orEmpty().map { it.text }
    }

    @Test
    fun daysAreNewestFirstEachWithItsDateAndVersion() {
        val days = whatsNewTimeline(
            listOf(release("0.8.0", at(10, 22)), release("1.0.0", at(11, 3)), release("0.9.0", at(10, 27))),
            installedVersion = "0.8.0",
            zone = utc,
        )

        assertEquals(listOf(LocalDate(2026, 11, 3), LocalDate(2026, 10, 27), LocalDate(2026, 10, 22)), days.map { it.date })
        assertEquals(listOf("1.0.0", "0.9.0", "0.8.0"), days.map { it.version })
        assertEquals(listOf(listOf("Version 1.0.0"), listOf("Version 0.9.0"), listOf("Version 0.8.0")), days.texts(ChangeType.New))
    }

    @Test
    fun releasesOfTheSameDayShowTheLastVersionWithTheChangesOfAll() {
        val days = whatsNewTimeline(
            listOf(
                release("1.0.1", at(11, 3, hour = 15), "### Bug fixes\n\n* Fix of 1.0.1"),
                release("1.0.0", at(11, 3, hour = 8), "### Features\n\n* Feature of 1.0.0\n\n### Bug fixes\n\n* Fix of 1.0.0"),
                release("1.0.2", at(11, 3, hour = 20), "### Features\n\n* Feature of 1.0.2"),
                release("0.9.0", at(11, 2)),
            ),
            installedVersion = "0.9.0",
            zone = utc,
        )

        assertEquals(listOf("1.0.2", "0.9.0"), days.map { it.version })
        val day = days.first()
        assertEquals(LocalDate(2026, 11, 3), day.date)
        assertEquals(listOf(ChangeType.New, ChangeType.Fixed), day.groups.map { it.type })
        // The newest release's changes first.
        assertEquals(listOf("Feature of 1.0.2", "Feature of 1.0.0"), day.groups[0].changes.map { it.text })
        assertEquals(listOf("Fix of 1.0.1", "Fix of 1.0.0"), day.groups[1].changes.map { it.text })
        assertEquals(listOf("1.0.1", "1.0.0"), day.alsoIncludes)
        assertEquals(emptyList(), days.last().alsoIncludes)
    }

    @Test
    fun theLastVersionOfADayIsTheHighestNotTheLastPublished() {
        // A fix for the previous line, published after the new one.
        val days = whatsNewTimeline(
            listOf(release("1.1.0", at(11, 3, hour = 8)), release("1.0.1", at(11, 3, hour = 18))),
            installedVersion = "1.0.0",
            zone = utc,
        )

        assertEquals("1.1.0", days.single().version)
        assertEquals(listOf(listOf("Version 1.1.0", "Version 1.0.1")), days.texts(ChangeType.New))
    }

    @Test
    fun theDayIsTheOneOfTheDeviceTimeZone() {
        val releases = listOf(release("1.0.1", at(10, 28, hour = 1)), release("1.0.0", at(10, 27, hour = 22, minute = 30)))

        val inUtc = whatsNewTimeline(releases, "0.9.0", utc)
        assertEquals(listOf(LocalDate(2026, 10, 28), LocalDate(2026, 10, 27)), inUtc.map { it.date })
        assertEquals(listOf("1.0.1", "1.0.0"), inUtc.map { it.version })

        // Two hours ahead both were published on the 28th: one day, the last version.
        val ahead = whatsNewTimeline(releases, "0.9.0", UtcOffset(hours = 2).asTimeZone())
        assertEquals(listOf(LocalDate(2026, 10, 28)), ahead.map { it.date })
        assertEquals("1.0.1", ahead.single().version)

        // Eight hours behind both were published on the 27th.
        val behind = whatsNewTimeline(releases, "0.9.0", UtcOffset(hours = -8).asTimeZone())
        assertEquals(listOf(LocalDate(2026, 10, 27)), behind.map { it.date })
    }

    @Test
    fun changesAreGroupedAsNewFixedAndOtherWithScopeAndLink() {
        val notes = """
            ## [1.0.0](https://github.com/beppe2880/opencode-mobile/compare/v0.9.0...v1.0.0) (2026-11-03)

            ### Performance Improvements

            * Faster start

            ### Bug fixes

            * **sessions:** Forget deleted sessions ([#87](https://github.com/beppe2880/opencode-mobile/issues/87))

            ### ⚠ BREAKING CHANGES

            * **api:** Require OpenCode 2.1

            ### Features

            * **chat:** Follow the agent's work live ([#72](https://github.com/beppe2880/opencode-mobile/issues/72))
            * Add a file viewer
        """.trimIndent()

        val day = whatsNewTimeline(listOf(release("1.0.0", at(11, 3), notes)), "0.9.0", utc).single()

        assertEquals(
            listOf(
                WhatsNewGroup(
                    ChangeType.New,
                    listOf(
                        WhatsNewChange("Follow the agent's work live", "chat", "https://github.com/beppe2880/opencode-mobile/issues/72"),
                        WhatsNewChange("Add a file viewer"),
                    ),
                ),
                WhatsNewGroup(
                    ChangeType.Fixed,
                    listOf(WhatsNewChange("Forget deleted sessions", "sessions", "https://github.com/beppe2880/opencode-mobile/issues/87")),
                ),
                WhatsNewGroup(ChangeType.Other, listOf(WhatsNewChange("Faster start"), WhatsNewChange("Require OpenCode 2.1", "api"))),
            ),
            day.groups,
        )
    }

    @Test
    fun headingsAreClassifiedAsNewFixedOrOther() {
        assertEquals(ChangeType.New, changeTypeOf(ReleaseSectionKind.Features))
        assertEquals(ChangeType.Fixed, changeTypeOf(ReleaseSectionKind.Fixes))
        assertEquals(ChangeType.Other, changeTypeOf(ReleaseSectionKind.Other))
        assertEquals(ChangeType.Other, changeTypeOf(ReleaseSectionKind.Breaking))

        // Hand-written notes with their own heading are Other.
        val day = whatsNewTimeline(listOf(release("0.3.0", at(9, 1), "### Highlights\n\n* Pair with a QR code")), "0.3.0", utc).single()
        assertEquals(listOf(WhatsNewGroup(ChangeType.Other, listOf(WhatsNewChange("Pair with a QR code")))), day.groups)
    }

    @Test
    fun aChangeRepeatedByTheReleasesOfADayIsListedOnce() {
        val days = whatsNewTimeline(
            listOf(
                release("1.0.1", at(11, 3, hour = 15), "### Bug fixes\n\n* **chat:** Same fix\n* Another fix"),
                release("1.0.0", at(11, 3, hour = 8), "### Bug fixes\n\n* **chat:** Same fix"),
            ),
            installedVersion = "0.9.0",
            zone = utc,
        )

        assertEquals(listOf(listOf("Same fix", "Another fix")), days.texts(ChangeType.Fixed))
    }

    @Test
    fun theDayOfTheInstalledVersionIsMarkedAndNewerDaysOfferTheUpdate() {
        val releases = listOf(release("1.1.0", at(11, 3)), release("1.0.0", at(10, 27)), release("0.9.0", at(10, 22)))

        val days = whatsNewTimeline(releases, installedVersion = "1.0.0", zone = utc)
        assertEquals(listOf(false, true, false), days.map { it.installed })
        assertEquals(listOf(true, false, false), days.map { it.updateAvailable })

        // A debug build of the same version is that version.
        assertEquals(listOf(false, true, false), whatsNewTimeline(releases, "1.0.0-kmp", utc).map { it.installed })

        val latest = whatsNewTimeline(releases, installedVersion = "1.1.0", zone = utc)
        assertEquals(listOf(true, false, false), latest.map { it.installed })
        assertTrue(latest.none { it.updateAvailable })
    }

    @Test
    fun anInstalledVersionOutsideTheHistoryMarksNoDay() {
        val releases = listOf(release("1.1.0", at(11, 3)), release("1.0.0", at(10, 27)))

        // Older than every release kept: all are updates.
        val old = whatsNewTimeline(releases, installedVersion = "0.5.0", zone = utc)
        assertTrue(old.none { it.installed })
        assertTrue(old.all { it.updateAvailable })

        // A build without a release version is never told to update.
        val dev = whatsNewTimeline(releases, installedVersion = "dev", zone = utc)
        assertTrue(dev.none { it.installed })
        assertTrue(dev.none { it.updateAvailable })
    }

    @Test
    fun theInstalledVersionSharingItsDayWithANewerOneIsMarkedAndOffersTheUpdate() {
        val day = whatsNewTimeline(
            listOf(release("1.0.1", at(11, 3, hour = 18)), release("1.0.0", at(11, 3, hour = 8))),
            installedVersion = "1.0.0",
            zone = utc,
        ).single()

        assertEquals("1.0.1", day.version)
        assertTrue(day.installed)
        assertTrue(day.updateAvailable)
    }

    @Test
    fun releasesWithoutADateOrAVersionHaveNoDay() {
        val days = whatsNewTimeline(
            listOf(release("1.0.0", at(11, 3)), release("0.9.0", publishedAt = null), release("nightly", at(11, 4))),
            installedVersion = "1.0.0",
            zone = utc,
        )

        assertEquals(listOf("1.0.0"), days.map { it.version })
        assertEquals(emptyList(), whatsNewTimeline(emptyList(), "1.0.0", utc))
    }

    @Test
    fun aReleaseWithoutStructuredNotesKeepsItsDayWithoutChanges() {
        val day = whatsNewTimeline(listOf(release("1.0.0", at(11, 3), notes = "Just a paragraph.")), "0.9.0", utc).single()

        assertEquals("1.0.0", day.version)
        assertEquals(emptyList(), day.groups)
        assertFalse(day.installed)
    }
}
