package io.github.beppe2880.opencodemobile.core.domain

/** What a group of release notes is about, from its heading. */
enum class ReleaseSectionKind { Breaking, Features, Fixes, Other }

/** A group of release notes under one heading; [title] as published, empty without a heading. */
data class ReleaseSection(val kind: ReleaseSectionKind, val title: String, val items: List<ReleaseNote>)

/** One change, as plain text; [scope] is the part of the app it concerns (`chat`), when given. */
data class ReleaseNote(val text: String, val scope: String? = null)

/**
 * Reads the release notes of a GitHub release, written by release-please:
 *
 * ```
 * ## [0.9.0](https://github.com/…/compare/v0.8.0...v0.9.0) (2026-10-27)
 * ### Features
 * * **chat:** Follow the agent's work live ([#72](…)) ([9d25372](…))
 * ```
 *
 * Each `###` heading starts a section of its bullet items, kept as plain text: the scope is taken
 * apart, commit hashes, issue references and links are dropped, Markdown emphasis is removed.
 * Other text (the version heading, paragraphs) is skipped, and sections without items too.
 */
object ReleaseNotes {

    private val Heading = Regex("""^\s{0,3}(#{1,6})\s+(.*?)\s*#*\s*$""")
    private val Bullet = Regex("""^(\s*)[*+-]\s+(.*)$""")

    // Conventional commit scopes are lowercase: `**New:**` in hand-written notes is emphasis.
    private val Scope = Regex("""^\*\*([a-z0-9][a-z0-9._/-]*):\*\*\s*""")
    private val LinkInParentheses = Regex("""\s*\(\[[^\]]*]\([^)]*\)\)""")
    private val Link = Regex("""\[([^\]]*)]\([^)]*\)""")
    private val Closes = Regex(""",?\s*\bcloses\s+#\d+(?:\s*(?:,|and)?\s*#\d+)*""", RegexOption.IGNORE_CASE)
    private val Reference = Regex("""\s*\((?:#\d+|[0-9a-f]{7,40})(?:\s*,\s*(?:#\d+|[0-9a-f]{7,40}))*\)""")
    private val Emphasis = Regex("""\*\*|__|`""")
    private val Spaces = Regex("""\s+""")

    fun parse(markdown: String): List<ReleaseSection> {
        val sections = mutableListOf<ReleaseSection>()
        var title: String? = null
        var items = mutableListOf<ReleaseNote>()
        // The raw text of the bullet being read: indented lines continue it.
        var item: StringBuilder? = null

        fun endItem() {
            item?.let { raw -> clean(raw.toString())?.let(items::add) }
            item = null
        }

        fun endSection() {
            endItem()
            if (items.isNotEmpty()) sections += ReleaseSection(kindOf(title.orEmpty()), title.orEmpty(), items)
            items = mutableListOf()
        }

        for (line in markdown.lineSequence()) {
            val heading = Heading.matchEntire(line)
            val bullet = Bullet.matchEntire(line)
            when {
                heading != null -> {
                    endSection()
                    // Level 1 and 2 headings name the version, not a group of changes.
                    title = if (heading.groupValues[1].length >= 3) plain(heading.groupValues[2]) else null
                }
                bullet != null -> {
                    endItem()
                    item = StringBuilder(bullet.groupValues[2])
                }
                line.isBlank() -> endItem()
                item != null && line.first().isWhitespace() -> item?.append(' ')?.append(line.trim())
                else -> endItem()
            }
        }
        endSection()
        return sections
    }

    private fun clean(raw: String): ReleaseNote? {
        var text = raw.trim()
        val scope = Scope.find(text)?.let { match ->
            text = text.substring(match.range.last + 1)
            plain(match.groupValues[1]).takeIf { it.isNotEmpty() }
        }
        text = text.replace(LinkInParentheses, "")
        text = text.replace(Link, "$1")
        text = text.replace(Closes, "")
        text = text.replace(Reference, "")
        text = plain(text)
        return if (text.isEmpty()) null else ReleaseNote(text, scope)
    }

    private fun plain(text: String): String = text.replace(Emphasis, "").replace(Spaces, " ").trim()

    private fun kindOf(title: String): ReleaseSectionKind {
        val key = title.lowercase().filter { it.isLetter() }
        return when {
            "breaking" in key -> ReleaseSectionKind.Breaking
            key == "features" || key == "feature" -> ReleaseSectionKind.Features
            key == "bugfixes" || key == "fixes" || key == "bugfix" -> ReleaseSectionKind.Fixes
            else -> ReleaseSectionKind.Other
        }
    }
}
