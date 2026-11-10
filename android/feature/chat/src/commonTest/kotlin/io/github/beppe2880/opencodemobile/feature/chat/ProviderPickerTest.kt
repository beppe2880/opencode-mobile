package io.github.beppe2880.opencodemobile.feature.chat

import io.github.beppe2880.opencodemobile.core.domain.FormField
import io.github.beppe2880.opencodemobile.core.domain.KeyIntegration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProviderPickerTest {
    private fun provider(id: String, name: String = id, connected: Boolean = false) =
        KeyIntegration(id, name, connected, null, emptyList())

    @Test
    fun groupsMatchOpenCodeOrderWithoutDuplicates() {
        val all = listOf(
            provider("google"), provider("custom", "Custom", true), provider("openai", "OpenAI", true),
            provider("anthropic"), provider("github-copilot"), provider("opencode-go"), provider("opencode"),
            provider("zeta", "Zeta"), provider("alpha", "Alpha"),
        )
        val groups = providerGroups(all, "")
        assertEquals(listOf(ProviderGroup.Connected, ProviderGroup.Popular, ProviderGroup.Other), groups.keys.toList())
        assertEquals(listOf("custom", "openai"), groups.getValue(ProviderGroup.Connected).map { it.id })
        assertEquals(listOf("opencode", "opencode-go", "github-copilot", "anthropic", "google"), groups.getValue(ProviderGroup.Popular).map { it.id })
        assertEquals(listOf("alpha", "zeta"), groups.getValue(ProviderGroup.Other).map { it.id })
        assertEquals(all.map { it.id }.sorted(), groups.values.flatten().map { it.id }.sorted())
    }

    @Test
    fun onlySecretLookingFormFieldsAreMasked() {
        fun field(key: String, title: String? = null) = FormField.Text(key = key, title = title)
        assertFalse(field("accountId", "Enter your Cloudflare Account ID").looksSecret())
        assertFalse(field("resourceName", "Enter Azure Resource Name").looksSecret())
        assertFalse(field("enterpriseUrl").looksSecret())
        assertFalse(field("server").looksSecret())
        assertTrue(field("secretAccessKey").looksSecret())
        assertTrue(field("token").looksSecret())
        assertTrue(field("extra", "Service API key").looksSecret())
    }

    @Test
    fun allPopularProvidersFollowCuratedOrder() {
        val order = listOf("opencode", "opencode-go", "openai", "github-copilot", "anthropic", "google")
        assertEquals(order, providerGroups(order.reversed().map { provider(it) }, "").getValue(ProviderGroup.Popular).map { it.id })
    }

    @Test
    fun searchMatchesNamesAndIdsIgnoringCaseAndSurroundingWhitespace() {
        val all = listOf(provider("github-copilot", "GitHub Copilot"), provider("custom", "My service", true), provider("openai"))
        assertEquals(listOf(all[0]), providerGroups(all, "  COPILOT ").values.flatten())
        assertEquals(listOf(all[1]), providerGroups(all, "my SERVICE").values.flatten())
        assertEquals(listOf(all[1]), providerGroups(all, "CUSTOM").values.flatten())
    }

    @Test
    fun emptyGroupsAreOmittedAndMissingPopularProvidersAreNotInvented() {
        val all = listOf(provider("custom"))
        assertEquals(mapOf(ProviderGroup.Other to all), providerGroups(all, ""))
        assertTrue(providerGroups(all, "missing").isEmpty())
        assertTrue(providerGroups(emptyList(), "").isEmpty())
    }

    @Test
    fun connectedAndOtherGroupsSortNamesCaseInsensitively() {
        val all = listOf(provider("z", "Zeta"), provider("a", "alpha"), provider("y", "Zebra", true), provider("b", "beta", true))
        val groups = providerGroups(all, "")
        assertEquals(listOf("b", "y"), groups.getValue(ProviderGroup.Connected).map { it.id })
        assertEquals(listOf("a", "z"), groups.getValue(ProviderGroup.Other).map { it.id })
    }

    @Test
    fun connectionStateMovesProviderBetweenGroups() {
        val openai = provider("openai")
        assertTrue(ProviderGroup.Popular in providerGroups(listOf(openai), ""))
        val groups = providerGroups(listOf(openai.copy(connected = true)), "")
        assertFalse(ProviderGroup.Popular in groups)
        assertEquals(listOf(openai.copy(connected = true)), groups.getValue(ProviderGroup.Connected))
    }

    @Test
    fun serverNamesAndAuthenticationDetailsArePreserved() {
        val integration = provider("openai", "Server supplied name").copy(keyLabel = "Server API key", hasKey = false)
        assertEquals(integration, providerGroups(listOf(integration), "").values.single().single())
    }
}
