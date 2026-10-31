package io.github.beppe2880.opencodemobile.core.network.mapping

import io.github.beppe2880.opencodemobile.api.models.ConfigModelEntry
import io.github.beppe2880.opencodemobile.core.domain.ModelRef
import io.github.beppe2880.opencodemobile.core.network.OpenCodeJson
import kotlinx.serialization.builtins.ListSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConfigMappingTest {

    private fun entries(json: String) = OpenCodeJson.decodeFromString(ListSerializer(ConfigModelEntry.serializer()), json)

    @Test
    fun `the last config document that sets a model wins, in either written form`() {
        val config = entries(
            """[{"type":"document","path":"/home/user/.config/opencode/opencode.json","info":{"model":"openai/gpt-5","theme":"x"}},""" +
                """{"type":"document","path":"/home/user/project/opencode.json",""" +
                """"info":{"model":{"providerID":"anthropic","model":"claude-haiku-4-5","variant":"high"}}},""" +
                """{"type":"document","path":"/home/user/project/.opencode/opencode.json","info":{"agent":{}}},""" +
                """{"type":"directory","path":"/home/user/project/.opencode"}]""",
        )
        assertEquals(ModelRef("claude-haiku-4-5", "anthropic", "high"), config.configuredModel())
        assertEquals(ModelRef("gpt-5", "openai"), config.take(1).configuredModel())
    }

    @Test
    fun `a string keeps its variant and the default variant means none`() {
        assertEquals(ModelRef("gpt-5", "openai", "low"), entries("""[{"type":"document","info":{"model":"openai/gpt-5#low"}}]""").configuredModel())
        assertEquals(ModelRef("a/b", "openrouter"), entries("""[{"type":"document","info":{"model":"openrouter/a/b"}}]""").configuredModel())
        assertEquals(
            ModelRef("m", "p"),
            entries("""[{"type":"document","info":{"model":{"providerID":"p","model":"m","variant":"default"}}}]""").configuredModel(),
        )
    }

    @Test
    fun `no model, a null model or a malformed one is no configured model`() {
        assertNull(entries("""[{"type":"document","info":{"model":null}},{"type":"directory","path":"/p"}]""").configuredModel())
        assertNull(entries("""[{"type":"document","info":{}}]""").configuredModel())
        assertNull(entries("""[{"type":"document","info":{"model":"no-provider"}}]""").configuredModel())
        assertNull(entries("""[{"type":"document","info":{"model":{"providerID":"p"}}}]""").configuredModel())
    }
}
