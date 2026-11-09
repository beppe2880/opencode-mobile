package io.github.beppe2880.opencodemobile.feature.chat

import io.github.beppe2880.opencodemobile.core.domain.ModelRef
import io.github.beppe2880.opencodemobile.feature.chat.resources.Res
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_model
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_model_description
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_model_description_variant
import kotlin.test.Test
import kotlin.test.assertEquals

class ModelButtonTest {
    private val opus = ModelRef("claude-opus-5-5", "anthropic")
    private val opusLabel = ModelLabel("Claude Opus 5.5", "Anthropic")

    @Test
    fun buttonShowsShortNameAndCapitalizedVariant() {
        assertEquals(ModelButtonText("Opus 5.5", "Medium"), modelButtonText(opus.copy(variant = "medium"), opusLabel))
        assertEquals(ModelButtonText("Opus 5.5", null), modelButtonText(opus, opusLabel))
    }

    @Test
    fun claudeServedByAnotherProviderKeepsItsFamily() {
        val copilot = ModelRef("claude-opus-5-5", "github-copilot", "high")
        assertEquals(ModelButtonText("Claude Opus 5.5", "High"), modelButtonText(copilot, ModelLabel("Claude Opus 5.5", "GitHub Copilot")))
    }

    @Test
    fun descriptionKeepsFullNameProviderAndLowercaseEffort() {
        assertEquals(
            ModelDescription(Res.string.chat_model_description_variant, listOf("Claude Opus 5.5", "Anthropic", "medium")),
            modelDescription(opus.copy(variant = "Medium"), opusLabel),
        )
    }

    @Test
    fun descriptionWithoutVariantLeavesOutTheEffort() {
        assertEquals(
            ModelDescription(Res.string.chat_model_description, listOf("Claude Opus 5.5", "Anthropic")),
            modelDescription(opus, opusLabel),
        )
    }

    @Test
    fun descriptionWithoutModelIsTheGenericLabel() {
        assertEquals(ModelDescription(Res.string.chat_model, emptyList()), modelDescription(null, null))
    }
}
