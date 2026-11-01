package io.github.beppe2880.opencodemobile.feature.settings.customize

import io.github.beppe2880.opencodemobile.core.domain.SettingsArea

/**
 * Options whose words contain every word of [query], ignoring case: those whose [title]
 * matches come first, then those matched by their [description] (which may include the names
 * of their area, group and part), each in Settings order. A blank query finds nothing.
 */
internal fun searchOptions(
    query: String,
    title: (SettingOption) -> String,
    description: (SettingOption) -> String,
): List<SettingOption> {
    val words = queryWords(query)
    if (words.isEmpty()) return emptyList()
    val byTitle = SettingOption.entries.filter { title(it).matches(words) }
    val byDescription = SettingOption.entries.filter { it !in byTitle && (title(it) + " " + description(it)).matches(words) }
    return byTitle + byDescription
}

/** The [areas] whose name or [description] contain every word of [query], in Settings order. */
internal fun searchAreas(query: String, areas: List<SettingsArea>, name: (SettingsArea) -> String, description: (SettingsArea) -> String): List<SettingsArea> {
    val words = queryWords(query)
    if (words.isEmpty()) return emptyList()
    return areas.filter { (name(it) + " " + description(it)).matches(words) }
}

private fun queryWords(query: String): List<String> = query.lowercase().split(' ').filter { it.isNotBlank() }

private fun String.matches(words: List<String>): Boolean = lowercase().let { lower -> words.all { it in lower } }
