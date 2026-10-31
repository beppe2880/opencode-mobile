package io.github.beppe2880.opencodemobile.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NewChatModelTest {

    private val haiku = ModelRef("haiku", "anthropic")
    private val opus = ModelRef("opus", "anthropic")
    private val gpt = ModelRef("gpt", "openai", "low")
    private val available = listOf(haiku, opus, gpt).map {
        ModelInfo(it.copy(variant = null), it.id, it.providerId, null, 1, true, emptyList(), 0, true, "active")
    }
    private fun agent(model: ModelRef?) = Agent("plan", "plan", null, AgentMode.Primary, hidden = false, color = null, model = model)

    @Test
    fun `the agent's model, then the configured one, then the last used, each only when available`() {
        assertEquals(opus, newChatModel(agent(opus), gpt, available, listOf(haiku)))
        assertEquals(gpt, newChatModel(agent(null), gpt, available, listOf(haiku)))
        assertEquals(haiku, newChatModel(null, null, available, listOf(haiku)))
        val gone = ModelRef("gone", "openai")
        assertEquals(haiku, newChatModel(agent(gone), gone, available, listOf(gone, haiku)))
    }

    @Test
    fun `nothing available leaves the choice to the server`() {
        assertNull(newChatModel(agent(null), null, available, emptyList()))
        assertNull(newChatModel(agent(opus), gpt, emptyList(), listOf(haiku)))
    }
}
