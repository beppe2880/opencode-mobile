package io.github.beppe2880.opencodemobile.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReleaseNotesTest {

    @Test
    fun releasePleaseNotesBecomeSectionsOfPlainItems() {
        val sections = ReleaseNotes.parse(ReleaseNotesFixtures.V0_9_0)

        assertEquals(listOf(ReleaseSectionKind.Features, ReleaseSectionKind.Fixes), sections.map { it.kind })
        assertEquals(listOf("Features", "Bug fixes"), sections.map { it.title })
        assertEquals(
            listOf(
                ReleaseNote("Add MCP status and connection controls", "chat"),
                ReleaseNote("Follow the agent's work live", "chat"),
                ReleaseNote("Add output, refresh, text size and reset options", "settings"),
                ReleaseNote("Show the keyboard when opening the terminal", "terminal"),
            ),
            sections[0].items,
        )
        assertEquals(7, sections[1].items.size)
        assertEquals(ReleaseNote("Keep reading position stable while streaming", "chat"), sections[1].items.first())
    }

    @Test
    fun itemsWithoutScopeKeepTheirWholeText() {
        val sections = ReleaseNotes.parse(ReleaseNotesFixtures.V0_8_0)

        assertEquals(
            listOf(
                ReleaseNote("Add a home screen widget with running and recent sessions"),
                ReleaseNote("Add a read-only file viewer"),
                ReleaseNote("Add a terminal through the PTY WebSocket"),
            ),
            sections.first { it.kind == ReleaseSectionKind.Features }.items,
        )
        assertEquals(
            listOf(ReleaseNote("Show the compaction summary in the transcript", "chat")),
            sections.first { it.kind == ReleaseSectionKind.Fixes }.items,
        )
    }

    @Test
    fun noHashLinkOrIssueNumberIsLeft() {
        val texts = listOf(ReleaseNotesFixtures.V0_9_0, ReleaseNotesFixtures.V0_8_0, ReleaseNotesFixtures.V0_3_0)
            .flatMap(ReleaseNotes::parse)
            .flatMap { it.items }
            .map { it.text }
        assertTrue(texts.isNotEmpty())
        texts.forEach { text ->
            assertTrue("http" !in text && "#" !in text && "](" !in text && "**" !in text && "`" !in text, text)
            assertTrue(!Regex("""\b[0-9a-f]{7}\b""").containsMatchIn(text), text)
        }
    }

    @Test
    fun handWrittenNotesKeepTheirHeadingsAndTextOfLinks() {
        val sections = ReleaseNotes.parse(ReleaseNotesFixtures.V0_3_0)

        assertEquals(listOf(ReleaseSection(ReleaseSectionKind.Other, "Highlights", sections.single().items)), sections)
        assertEquals("Pair with opencode pair (QR code or link), a password, or find servers on your Wi-Fi.", sections.single().items.first().text)
        assertEquals("New: fork a session from any prompt, or copy it whole with /fork.", sections.single().items[3].text)
    }

    @Test
    fun otherSectionsBreakingChangesAndClosedIssuesAreRead() {
        val notes = """
            ## 1.0.0 (2026-11-01)

            ### ⚠ BREAKING CHANGES

            * **api:** Require OpenCode 2.1
              on the server ([#140](https://example.com/140)), closes [#12](https://example.com/12) [#13](https://example.com/13)

            ### Performance Improvements

            - Faster start (#141) (abc1234)

            Some paragraph that is not a change.
        """.trimIndent()

        assertEquals(
            listOf(
                ReleaseSection(ReleaseSectionKind.Breaking, "⚠ BREAKING CHANGES", listOf(ReleaseNote("Require OpenCode 2.1 on the server", "api"))),
                ReleaseSection(ReleaseSectionKind.Other, "Performance Improvements", listOf(ReleaseNote("Faster start"))),
            ),
            ReleaseNotes.parse(notes),
        )
    }

    @Test
    fun emptyOrUnstructuredNotesHaveNoSections() {
        assertEquals(emptyList(), ReleaseNotes.parse(""))
        assertEquals(emptyList(), ReleaseNotes.parse("Just a paragraph.\n\n### Empty\n"))
    }
}
