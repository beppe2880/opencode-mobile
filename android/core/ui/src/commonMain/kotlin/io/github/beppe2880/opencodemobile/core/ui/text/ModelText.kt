package io.github.beppe2880.opencodemobile.core.ui.text

/**
 * Brand words that a provider's mark already shows, by provider id. Only providers with a drawn
 * mark are listed (the same ids as the provider marks of the chat): with the initial fallback the
 * brand is not visible, so the name stays whole.
 *
 * A model family is listed only when it belongs to that provider alone: "Claude" is Anthropic's,
 * so "Claude Opus 5.5" from Anthropic becomes "Opus 5.5". "Gemini" and "GPT" stay, because the
 * Google and OpenAI marks name the company rather than that family, and a Claude model served by
 * GitHub Copilot or OpenCode keeps "Claude" because their marks do not show Anthropic.
 */
private val markBrands: Map<String, List<String>> = mapOf(
    "anthropic" to listOf("Anthropic", "Claude"),
    "openai" to listOf("OpenAI"),
    "google" to listOf("Google"),
    "github-copilot" to listOf("GitHub Copilot", "Copilot"),
    "opencode-go" to listOf("OpenCode Go", "OpenCode"),
    "opencode" to listOf("OpenCode"),
)

private val brandSeparators = charArrayOf(' ', '-', ':', '/', '·')

/**
 * The model [name] without the leading brand that the mark of [providerId] already shows:
 * "Claude Opus 5.5" from `anthropic` is "Opus 5.5". A brand is stripped only as a whole word
 * (followed by a space, `-`, `:`, `/` or `·`, ignoring case) and only when a name starting with a
 * letter remains, so "Claude 3 Opus" and a bare "Claude" stay whole. Names of other providers are
 * returned as they are.
 */
fun shortModelName(name: String, providerId: String): String {
    val brands = markBrands[providerId] ?: return name
    var rest = name.trim()
    var stripped = false
    while (true) {
        val brand = brands.firstOrNull { brand ->
            rest.length > brand.length && rest.startsWith(brand, ignoreCase = true) && rest[brand.length] in brandSeparators
        } ?: break
        rest = rest.substring(brand.length).trimStart(*brandSeparators)
        stripped = true
    }
    return if (stripped && rest.firstOrNull()?.isLetter() == true) rest else name
}

/** A reasoning variant as the model picker shows it: `medium` is "Medium". */
fun variantLabel(variant: String): String = variant.replaceFirstChar { it.titlecase() }
