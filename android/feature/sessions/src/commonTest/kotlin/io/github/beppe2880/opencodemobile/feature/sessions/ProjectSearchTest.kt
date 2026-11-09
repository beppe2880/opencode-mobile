package io.github.beppe2880.opencodemobile.feature.sessions

import io.github.beppe2880.opencodemobile.core.domain.Project
import kotlin.test.Test
import kotlin.test.assertEquals

class ProjectSearchTest {
    private fun project(id: String, directory: String, name: String? = null) =
        Project(id = id, directory = directory, name = name, vcs = null, updatedAt = 0)

    private val many = (1..8).map { project("p$it", "/home/me/repo$it") }

    @Test
    fun fewProjectsAreAllShown() {
        val projects = many.take(COLLAPSED_PROJECTS)
        assertEquals(projects, visibleProjects(projects, query = "", showAll = false, selectedDirectory = ""))
    }

    @Test
    fun manyProjectsCollapseKeepingTheSelectedOne() {
        val visible = visibleProjects(many, query = "", showAll = false, selectedDirectory = "/home/me/repo8")
        assertEquals(listOf("p1", "p2", "p3", "p4", "p5", "p8"), visible.map { it.id })
    }

    @Test
    fun showAllListsEveryProject() {
        assertEquals(many, visibleProjects(many, query = "", showAll = true, selectedDirectory = ""))
    }

    @Test
    fun blankQueryDoesNotSearch() {
        val visible = visibleProjects(many, query = "   ", showAll = false, selectedDirectory = "")
        assertEquals(COLLAPSED_PROJECTS, visible.size)
    }

    @Test
    fun queryMatchesDisplayNameIgnoringCase() {
        val projects = many + project("named", "/srv/x", name = "Mobile App")
        val visible = visibleProjects(projects, query = "mobile", showAll = false, selectedDirectory = "")
        assertEquals(listOf("named"), visible.map { it.id })
    }

    @Test
    fun querySearchesBeyondTheCollapsedProjects() {
        val visible = visibleProjects(many, query = "REPO7", showAll = false, selectedDirectory = "")
        assertEquals(listOf("p7"), visible.map { it.id })
    }

    @Test
    fun queryMatchesTheDirectoryPath() {
        val projects = listOf(project("a", "/work/client/api", name = "Backend"), project("b", "/home/me/notes"))
        val visible = visibleProjects(projects, query = "client", showAll = false, selectedDirectory = "")
        assertEquals(listOf("a"), visible.map { it.id })
    }

    @Test
    fun everyWordMustMatchNameOrPath() {
        val projects = listOf(project("a", "/work/client/api", name = "Backend"), project("b", "/work/client/web", name = "Frontend"))
        val visible = visibleProjects(projects, query = " backend  client ", showAll = false, selectedDirectory = "")
        assertEquals(listOf("a"), visible.map { it.id })
    }

    @Test
    fun noMatchGivesAnEmptyList() {
        assertEquals(emptyList<Project>(), visibleProjects(many, query = "missing", showAll = true, selectedDirectory = "/home/me/repo1"))
    }
}
