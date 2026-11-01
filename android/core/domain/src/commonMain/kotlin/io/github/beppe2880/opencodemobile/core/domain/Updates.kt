package io.github.beppe2880.opencodemobile.core.domain

/**
 * A published release of the app: its version (tag), the page to download it from and its
 * release notes as published (Markdown, read with [ReleaseNotes.parse]).
 */
data class AppRelease(val version: String, val url: String, val notes: String = "")

/** A newer release of the app than the installed one. */
data class AppUpdate(
    /** The newest release: the one to install and to skip. */
    val latest: AppRelease,
    /** Every known release newer than the installed version, newest first; starts with [latest]. */
    val releases: List<AppRelease> = listOf(latest),
)

/** New releases of the app, published on GitHub. */
interface UpdateRepository {
    /**
     * The releases newer than [installedVersion] when the newest was not dismissed, else null.
     * Asks GitHub at most once a day; in between, and when it cannot be reached, the releases
     * found by the last check are used.
     */
    suspend fun availableUpdate(installedVersion: String): AppUpdate?

    /** Hides [release] and older ones; a newer release is shown again. */
    suspend fun dismiss(release: AppRelease)
}

/** How the app announces a newer release when it opens. */
enum class UpdateNoticeStyle {
    /** A sheet with what changed, once per app start. */
    Window,

    /** A notice above the session list. */
    Banner,

    /** Never looks for new releases. */
    Off,
}

/**
 * A version such as `0.9.0`, `v1.2`, `0.9.0-kmp` or `1.0.0+build.5`. Only the numeric parts are
 * compared: a suffix marks a pre-release (`-`) or build metadata (`+`), so a local or debug build
 * like `0.9.0-kmp` is not told that `0.9.0` is newer.
 */
class AppVersion private constructor(
    private val numbers: List<Int>,
    /** The version has a `-` suffix (`1.0.0-beta`): never offered as an update. */
    val isPreRelease: Boolean,
) : Comparable<AppVersion> {

    override fun compareTo(other: AppVersion): Int {
        for (i in 0 until maxOf(numbers.size, other.numbers.size)) {
            val diff = numbers.getOrElse(i) { 0 }.compareTo(other.numbers.getOrElse(i) { 0 })
            if (diff != 0) return diff
        }
        return 0
    }

    override fun equals(other: Any?): Boolean = other is AppVersion && compareTo(other) == 0
    override fun hashCode(): Int = numbers.dropLastWhile { it == 0 }.hashCode()

    companion object {
        private val Pattern = Regex("""^[vV]?(\d{1,9}(?:\.\d{1,9}){0,3})(-[0-9A-Za-z.-]+)?(\+[0-9A-Za-z.-]+)?$""")

        /** Null when [text] is not a version. */
        fun parse(text: String): AppVersion? {
            val match = Pattern.matchEntire(text.trim()) ?: return null
            val numbers = match.groupValues[1].split('.').map { it.toInt() }
            return AppVersion(numbers, isPreRelease = match.groupValues[2].isNotEmpty())
        }

        /**
         * [release] is a stable version newer than [installed]. False when either is not a
         * version: unknown builds are not asked to update.
         */
        fun isUpdate(release: String, installed: String): Boolean {
            val candidate = parse(release) ?: return false
            val current = parse(installed) ?: return false
            return !candidate.isPreRelease && candidate > current
        }
    }
}
