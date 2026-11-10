package io.github.beppe2880.opencodemobile.core.ui.text

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelTextTest {

    @Test
    fun `brand shown by the mark is stripped`() {
        val cases = listOf(
            Triple("Claude Opus 5.5", "anthropic", "Opus 5.5"),
            Triple("Claude Haiku 5.5", "anthropic", "Haiku 5.5"),
            Triple("Anthropic: Claude Sonnet 4.5", "anthropic", "Sonnet 4.5"),
            Triple("claude-sonnet-4", "anthropic", "sonnet-4"),
            Triple("OpenAI o3", "openai", "o3"),
            Triple("GitHub Copilot Chat", "github-copilot", "Chat"),
            Triple("OpenCode Go Large", "opencode-go", "Large"),
        )
        cases.forEach { (name, provider, expected) -> assertEquals(name, expected, shortModelName(name, provider)) }
    }

    @Test
    fun `families that the mark does not name stay`() {
        assertEquals("Gemini 3 Pro", shortModelName("Gemini 3 Pro", "google"))
        assertEquals("GPT-5.1 Codex", shortModelName("GPT-5.1 Codex", "openai"))
        // Copilot and OpenCode marks do not show Anthropic.
        assertEquals("Claude Opus 5.5", shortModelName("Claude Opus 5.5", "github-copilot"))
        assertEquals("Claude Opus 5.5", shortModelName("Claude Opus 5.5", "opencode"))
    }

    @Test
    fun `providers without a mark keep the whole name`() {
        assertEquals("Mistral Large 2", shortModelName("Mistral Large 2", "mistral"))
        assertEquals("Claude Opus 5.5", shortModelName("Claude Opus 5.5", "openrouter"))
    }

    @Test
    fun `name is kept when stripping would leave a number or nothing`() {
        assertEquals("Claude 3 Opus", shortModelName("Claude 3 Opus", "anthropic"))
        assertEquals("Claude", shortModelName("Claude", "anthropic"))
        assertEquals("Claude ", shortModelName("Claude ", "anthropic"))
        assertEquals("Claudette", shortModelName("Claudette", "anthropic"))
    }

    @Test
    fun `variants are capitalized like the picker shows them`() {
        assertEquals("Medium", variantLabel("medium"))
        assertEquals("High", variantLabel("high"))
        assertEquals("Xhigh", variantLabel("xhigh"))
        assertEquals("", variantLabel(""))
    }
}
