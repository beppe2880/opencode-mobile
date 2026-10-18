package io.github.beppe2880.opencodemobile.core.network.mapping

import io.github.beppe2880.opencodemobile.api.models.ExperimentalSessionStats200Response
import io.github.beppe2880.opencodemobile.core.domain.DailyActivity
import io.github.beppe2880.opencodemobile.core.domain.ModelRef
import io.github.beppe2880.opencodemobile.core.network.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Test

class UsageMappingTest {

    private fun tokens(input: Int) = """{"input":$input,"output":1,"reasoning":0,"cache":{"read":0,"write":0}}"""

    @Test
    fun `variants of a model are merged and models sorted by cost`() {
        val json = """
            {"data":{"range":{"from":0,"to":1},"sessions":2,"subagents":1,"prompts":5,"steps":9,"cost":3.5,
             "tokens":${tokens(30)},"tools":{"mode":"none"},"activeDays":2,"streak":1,
             "activity":[{"date":"2026-10-18","steps":4},{"date":"2026-10-17","steps":5}],
             "models":[
               {"model":{"id":"haiku","providerID":"anthropic"},"steps":2,"tokens":${tokens(5)},"cost":0.5},
               {"model":{"id":"opus","providerID":"anthropic","variant":"high"},"steps":3,"tokens":${tokens(10)},"cost":1.0},
               {"model":{"id":"opus","providerID":"anthropic"},"steps":4,"tokens":${tokens(15)},"cost":2.0}
             ]}}
        """.trimIndent()

        val stats = Fixtures.json.decodeFromString<ExperimentalSessionStats200Response>(json).data.toDomain()

        assertEquals(listOf("2026-10-17", "2026-10-18"), stats.activity.map(DailyActivity::date))
        assertEquals(listOf(ModelRef("opus", "anthropic"), ModelRef("haiku", "anthropic")), stats.models.map { it.model })
        val opus = stats.models.first()
        assertEquals(7L, opus.steps)
        assertEquals(3.0, opus.cost, 1e-9)
        assertEquals(25L, opus.tokens.input)
    }
}
