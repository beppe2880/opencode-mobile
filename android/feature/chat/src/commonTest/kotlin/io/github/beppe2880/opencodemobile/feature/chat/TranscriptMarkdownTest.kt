package io.github.beppe2880.opencodemobile.feature.chat

import io.github.beppe2880.opencodemobile.core.domain.ImageAttachment
import io.github.beppe2880.opencodemobile.core.domain.Message
import io.github.beppe2880.opencodemobile.core.domain.ModelRef
import io.github.beppe2880.opencodemobile.core.domain.Part
import io.github.beppe2880.opencodemobile.core.domain.ToolStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TranscriptMarkdownTest {
    @Test
    fun preservesMathDelimitersAndLatexExactly() {
        val formulas = listOf(
            "Inline: ${'$'}E = mc^2${'$'} and ${'$'}\\alpha + \\beta${'$'}.",
            "${'$'}${'$'}\n\\int_0^1 x^2\\,dx = \\frac{1}{3}\n${'$'}${'$'}",
            "\\(a^2 + b^2 = c^2\\)",
            "\\[\\sum_{n=1}^{\\infty} \\frac{1}{n^2} = \\frac{\\pi^2}{6}\\]",
            "${'$'}${'$'}\n\\begin{pmatrix}\n1 & 2 \\\\\n3 & 4\n\\end{pmatrix}\n${'$'}${'$'}",
            "${'$'}${'$'}\n\\begin{aligned}\nf(x) &= x^2 \\\\\nf'(x) &= 2x\n\\end{aligned}\n${'$'}${'$'}",
            "Price: ${'$'}5; escaped currency: \\${'$'}10; Unicode: α × β ≤ ∞.",
        )
        for (formula in formulas) {
            val messages = listOf(
                Message.User("u", 0, formula),
                Message.Assistant("a", 1, "build", ModelRef("test", "test"), listOf(Part.Text(0, formula))),
            )
            assertEquals("# Math\n\n## User\n\n$formula\n\n## Assistant\n\n$formula\n\n", transcriptMarkdown("Math", messages))
        }
    }

    @Test
    fun preservesMathAlongsideTablesListsLinksAndFencedCode() {
        val richText = """
            Inline: ${'$'}E = mc^2${'$'}.

            ${'$'}${'$'}
            \int_0^1 x^2\,dx = \frac{1}{3}
            ${'$'}${'$'}

            ${'$'}${'$'}
            \begin{pmatrix}
            1 & 2 \\
            3 & 4
            \end{pmatrix}
            ${'$'}${'$'}

            ${'$'}${'$'}
            \begin{aligned}
            f(x) &= x^2 \\
            f'(x) &= 2x
            \end{aligned}
            ${'$'}${'$'}

            | Symbol | Formula |
            | --- | --- |
            | Area | ${'$'}\pi r^2${'$'} |

            - **Formula:** ${'$'}a^2 + b^2 = c^2${'$'}
            - [Reference](https://example.com/math)

            > Unicode: α × β ≤ ∞.

            ```python
            formula = r"${'$'}\frac{a}{b}${'$'}"
            price = "${'$'}5"
            ```
        """.trimIndent()
        val message = Message.Assistant("a", 0, "build", ModelRef("test", "test"), listOf(Part.Text(0, richText)))
        assertEquals("# Math\n\n## Assistant\n\n$richText\n\n", transcriptMarkdown("Math", listOf(message)))
    }

    @Test
    fun mathAndMarkdownInsideToolsRemainLiteralFencedOutput() {
        val output = "${'$'}${'$'}\\frac{a}{b}${'$'}${'$'}\n\\[x^2\\]\n```latex\n\\alpha\n```\n</details>"
        val message = Message.Assistant(
            "a", 0, "build", ModelRef("test", "test"),
            listOf(Part.Tool("t", "math <check>", ToolStatus.Completed, "{}", output)),
        )
        assertEquals(
            "# Math\n\n## Assistant\n\n<details>\n<summary>math &lt;check&gt;</summary>\n\n```\n{}\n```\n\n````\n$output\n````\n\n</details>\n\n",
            transcriptMarkdown("Math", listOf(message)),
        )
    }

    @Test
    fun exportsTextAndCollapsesToolsWithoutImagesOrReasoning() {
        val messages = listOf(
            Message.User(
                "u", 0, "A **question**",
                images = listOf(ImageAttachment("private.png", "image/png", "data:image/png;base64,secret")),
            ),
            Message.Assistant(
                "a", 1, "build", ModelRef("cheap", "test"),
                listOf(
                    Part.Text(0, "An answer"),
                    Part.Reasoning(1, "private reasoning"),
                    Part.Tool("t", "<shell>", ToolStatus.Completed, "{}", "```code```"),
                ),
            ),
            Message.Synthetic("s", 2),
        )
        val markdown = transcriptMarkdown("Title", messages)
        assertTrue(markdown.startsWith("# Title\n"))
        assertTrue(markdown.contains("A **question**"))
        assertTrue(markdown.contains("An answer"))
        assertTrue(markdown.contains("<details>\n<summary>&lt;shell&gt;</summary>"))
        assertTrue(markdown.contains("````\n```code```\n````"))
        assertFalse(markdown.contains("private reasoning"))
        assertFalse(markdown.contains("base64"))
        assertFalse(markdown.contains("private.png"))
    }

    @Test
    fun filenamesAreBoundedAndCannotEscapeTheExportDirectory() {
        assertEquals("OpenCode.md", markdownFileName("..."))
        assertFalse(markdownFileName("../../a/b\\c").contains('/'))
        assertTrue(markdownFileName("a".repeat(200)).length <= 83)
        assertEquals("Conversazione italiana.md", markdownFileName("Conversazione italiana"))
    }

    @Test
    fun historyActionsDisableWhileBusyOrDisconnected() {
        val idle = ChatState(directory = "/scratch", loading = false)
        assertTrue(idle.historyReady)
        assertFalse(idle.copy(running = true).historyReady)
        assertFalse(idle.copy(compacting = true).historyReady)
        assertFalse(idle.copy(reverting = true).historyReady)
        assertFalse(idle.copy(connection = io.github.beppe2880.opencodemobile.core.domain.ConnectionState.Disconnected).menuReady)
        assertFalse(idle.copy(creatingSession = true).menuReady)
    }
}
