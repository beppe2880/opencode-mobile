package io.github.beppe2880.opencodemobile.feature.chat

import io.github.beppe2880.opencodemobile.core.domain.Command
import io.github.beppe2880.opencodemobile.core.domain.PromptAttachment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CompletionsTest {

    @Test
    fun `a slash prompt splits into command and arguments`() {
        assertEquals(Completions.Slash("review", "the diff\nplease"), Completions.slash("/review  the diff\nplease"))
        assertEquals(Completions.Slash("undo", ""), Completions.slash("/undo"))
        assertNull(Completions.slash("plain text"))
    }

    @Test
    fun `a bang prompt is a shell command`() {
        assertEquals("ls -la", Completions.shell("  !ls -la "))
        assertEquals("", Completions.shell("!"))
        assertNull(Completions.shell("run !ls"))
    }

    @Test
    fun `slash suggestions list built-ins first and filter by prefix`() {
        val commands = listOf(Command("review", "Review changes"), Command("release", null))

        assertEquals(listOf("undo", "redo", "compact", "fork", "review", "release"), Completions.slashSuggestions("/", commands)?.map { it.name })
        assertEquals(listOf("review", "release"), Completions.slashSuggestions("/RE", commands)?.map { it.name }?.filter { it != "redo" })
        assertNull(Completions.slashSuggestions("/review now", commands))
        assertNull(Completions.slashSuggestions("hello", commands))
    }

    @Test
    fun `only the last word starting with at is a mention`() {
        assertEquals("src/Ma", Completions.mentionQuery("look at @src/Ma"))
        assertEquals("", Completions.mentionQuery("@"))
        assertNull(Completions.mentionQuery("mail me@example.com"))
        assertNull(Completions.mentionQuery("@done and more"))
    }

    @Test
    fun `picking a file completes the mention and attaches it`() {
        val (draft, attachment) = Completions.pickFile("look at @Ma", "/repo/", "src/Main.kt")!!

        assertEquals("look at @src/Main.kt ", draft)
        assertEquals(PromptAttachment("file:///repo/src/Main.kt", "src/Main.kt", mention = "@src/Main.kt"), attachment)
    }
}
