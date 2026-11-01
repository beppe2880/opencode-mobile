package io.github.beppe2880.opencodemobile.feature.settings.customize

/**
 * Options whose words contain every word of [query], ignoring case: those whose [title]
 * matches come first, then those matched by their [description] (which may include the names
 * of their area and part), each in Settings order. A blank query finds nothing.
 */
internal fun searchOptions(
    query: String,
    title: (SettingOption) -> String,
    description: (SettingOption) -> String,
): List<SettingOption> {
    val words = query.lowercase().split(' ').filter { it.isNotBlank() }
    if (words.isEmpty()) return emptyList()
    fun matches(text: String) = text.lowercase().let { lower -> words.all { it in lower } }
    val byTitle = SettingOption.entries.filter { matches(title(it)) }
    val byDescription = SettingOption.entries.filter { it !in byTitle && matches(title(it) + " " + description(it)) }
    return byTitle + byDescription
}
