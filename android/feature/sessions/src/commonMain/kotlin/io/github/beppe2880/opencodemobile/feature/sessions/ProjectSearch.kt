package io.github.beppe2880.opencodemobile.feature.sessions

import io.github.beppe2880.opencodemobile.core.domain.Project

/** Projects listed before "Show all" in the New session sheet; above it the search field appears. */
internal const val COLLAPSED_PROJECTS = 5

/**
 * Projects shown in the New session sheet.
 *
 * A non-blank [query] searches all projects: every word must appear, ignoring case, in the
 * display name or the directory. Otherwise the list is collapsed to [COLLAPSED_PROJECTS] unless
 * [showAll], keeping the project of [selectedDirectory] visible.
 */
internal fun visibleProjects(
    projects: List<Project>,
    query: String,
    showAll: Boolean,
    selectedDirectory: String,
): List<Project> {
    val words = query.lowercase().split(' ', '\t', '\n').filter { it.isNotEmpty() }
    return when {
        words.isNotEmpty() -> projects.filter { project ->
            val text = (project.displayName + " " + project.directory).lowercase()
            words.all { it in text }
        }
        showAll || projects.size <= COLLAPSED_PROJECTS -> projects
        else -> (projects.take(COLLAPSED_PROJECTS) + projects.filter { it.directory == selectedDirectory }).distinctBy { it.id }
    }
}
