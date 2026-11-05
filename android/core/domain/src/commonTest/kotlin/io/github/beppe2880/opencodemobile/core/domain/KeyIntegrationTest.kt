package io.github.beppe2880.opencodemobile.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KeyIntegrationTest {
    @Test fun deviceCompatibilityIsMethodSpecific() {
        assertTrue(deviceOAuthSupported("github-copilot", "device"))
        assertTrue(deviceOAuthSupported("openai", "chatgpt-headless"))
        assertTrue(deviceOAuthSupported("opencode", "device"))
        assertTrue(deviceOAuthSupported("xai", "device"))
        assertFalse(deviceOAuthSupported("openai", "chatgpt-token-sharing"))
        assertFalse(deviceOAuthSupported("openai", "chatgpt-browser"))
        assertFalse(deviceOAuthSupported("unknown", "device"))
    }

    @Test
    fun hiddenDefaultsFollowConditionsAndInactiveAnswersAreOmitted() {
        val integration = KeyIntegration(
            "custom", "Custom", false, null,
            listOf(
                FormField.Text("region", default = "eu"),
                FormField.Text("endpoint", default = "preset", conditions = listOf(FormCondition("region", true, FormAnswer.Text("eu")))),
                FormField.Text("project", required = true, conditions = listOf(FormCondition("endpoint", true, FormAnswer.Text("preset")))),
            ),
            hidden = setOf("endpoint"),
        )
        val initial = integration.defaults()
        assertFalse(integration.isComplete(initial))
        val values = initial + ("project" to FormAnswer.Text("demo"))
        assertTrue(integration.isComplete(values))
        assertEquals(FormAnswer.Text("preset"), integration.answers(values)["endpoint"])
        val changed = values + ("region" to FormAnswer.Text("us"))
        assertEquals(mapOf("region" to FormAnswer.Text("us")), integration.answers(changed))
        assertTrue(integration.isComplete(changed))
    }

    @Test
    fun validateTextNumbersAndMultipleChoices() {
        val integration = KeyIntegration(
            "any", "Any", false, null,
            listOf(
                FormField.Text("host", required = true, minLength = 3, pattern = "^https://"),
                FormField.Number("count", required = true, integer = true, min = 1.0, max = 4.0),
                FormField.MultiSelect("regions", required = true, options = emptyList(), custom = true, minItems = 1, maxItems = 2),
            ),
        )
        val good = mapOf("host" to FormAnswer.Text("https://example.test"), "count" to FormAnswer.Number(2.0), "regions" to FormAnswer.Choices(listOf("eu")))
        assertTrue(integration.isComplete(good))
        assertFalse(integration.isComplete(good + ("count" to FormAnswer.Number(1.5))))
        assertFalse(integration.isComplete(good + ("host" to FormAnswer.Text("http://example.test"))))
        assertFalse(integration.isComplete(good + ("regions" to FormAnswer.Choices(listOf("a", "b", "c")))))
    }
}
