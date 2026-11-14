package io.github.beppe2880.opencodemobile.feature.settings.whatsnew

import androidx.compose.runtime.Immutable
import io.github.beppe2880.opencodemobile.core.domain.AppRelease
import io.github.beppe2880.opencodemobile.core.domain.AppVersion
import io.github.beppe2880.opencodemobile.core.domain.ReleaseNotes
import io.github.beppe2880.opencodemobile.core.domain.ReleaseSectionKind
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** What a change is for the reader of the What's New page, in the order the page lists them. */
enum class ChangeType {
    /** A feature. */
    New,

    /** A bug fix. */
    Fixed,

    /** Anything else: performance, breaking changes, hand-written highlights. */
    Other,
}

/** One change of a release; [url] is its issue or pull request on GitHub, when the notes link one. */
@Immutable
data class WhatsNewChange(val text: String, val scope: String? = null, val url: String? = null)

/** The changes of one [type] published on a day. */
@Immutable
data class WhatsNewGroup(val type: ChangeType, val changes: List<WhatsNewChange>)

/**
 * A day on which the app was released, in the device time zone, with the last version published
 * that day and the changes of every release of that day under it.
 */
@Immutable
data class WhatsNewDay(
    val date: LocalDate,
    /** The highest version published on [date]. */
    val version: String,
    /** The changes of all the releases of [date] by type; empty when none has structured notes. */
    val groups: List<WhatsNewGroup>,
    /** The installed version was published on [date]. */
    val installed: Boolean = false,
    /** [version] is newer than the installed one: the day offers the update. */
    val updateAvailable: Boolean = false,
    /** The other versions published on [date], newest first; their changes are in [groups] too. */
    val alsoIncludes: List<String> = emptyList(),
)

/** How the heading of a group of release notes is shown on the page. */
fun changeTypeOf(kind: ReleaseSectionKind): ChangeType = when (kind) {
    ReleaseSectionKind.Features -> ChangeType.New
    ReleaseSectionKind.Fixes -> ChangeType.Fixed
    ReleaseSectionKind.Breaking, ReleaseSectionKind.Other -> ChangeType.Other
}

/**
 * The timeline of the What's New page: [releases] grouped by the day they were published in
 * [zone], newest day first. Each day carries one version, the highest published that day, and the
 * changes of all its releases merged by type (the newest release's first, without repeats).
 * Releases without a version or a publish date have no place on it. The day of
 * [installedVersion] is marked, and so are the days whose version is newer than it; a build
 * without a release version marks none.
 */
fun whatsNewTimeline(releases: List<AppRelease>, installedVersion: String, zone: TimeZone): List<WhatsNewDay> {
    val installed = AppVersion.parse(installedVersion)
    return releases
        .mapNotNull { release ->
            val version = AppVersion.parse(release.version) ?: return@mapNotNull null
            val publishedAt = release.publishedAt ?: return@mapNotNull null
            Dated(Instant.fromEpochMilliseconds(publishedAt).toLocalDateTime(zone).date, version, release)
        }
        .groupBy { it.date }
        .map { (date, published) ->
            val newestFirst = published.sortedByDescending { it.version }
            val last = newestFirst.first()
            WhatsNewDay(
                date = date,
                version = last.release.version,
                groups = mergedChanges(newestFirst.map { it.release }),
                installed = installed != null && newestFirst.any { it.version == installed },
                updateAvailable = AppVersion.isUpdate(last.release.version, installedVersion),
                alsoIncludes = newestFirst.drop(1).map { it.release.version },
            )
        }
        .sortedByDescending { it.date }
}

private class Dated(val date: LocalDate, val version: AppVersion, val release: AppRelease)

/** The changes of [releases] by [ChangeType], in the order of [ChangeType]; types without changes are left out. */
private fun mergedChanges(releases: List<AppRelease>): List<WhatsNewGroup> {
    val byType = LinkedHashMap<ChangeType, LinkedHashSet<WhatsNewChange>>()
    for (release in releases) {
        for (section in ReleaseNotes.parse(release.notes)) {
            val changes = byType.getOrPut(changeTypeOf(section.kind)) { LinkedHashSet() }
            section.items.forEach { changes += WhatsNewChange(it.text, it.scope, it.url) }
        }
    }
    return ChangeType.entries.mapNotNull { type ->
        byType[type]?.takeIf { it.isNotEmpty() }?.let { WhatsNewGroup(type, it.toList()) }
    }
}
