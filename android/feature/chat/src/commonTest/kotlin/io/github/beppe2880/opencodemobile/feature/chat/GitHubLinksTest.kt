package io.github.beppe2880.opencodemobile.feature.chat

import io.github.beppe2880.opencodemobile.core.domain.Part
import io.github.beppe2880.opencodemobile.core.domain.ToolStatus
import io.github.beppe2880.opencodemobile.feature.chat.GitHubLink.Kind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GitHubLinksTest {

    private fun shell(command: String, output: String? = null, status: ToolStatus = ToolStatus.Completed, callId: String = "c1") =
        Part.Tool(callId, "bash", status, input = """{"command":"$command","description":"Run it"}""", output = output)

    private fun summary(links: List<GitHubLink>) = links.map { it.kind to it.ref }

    @Test
    fun `gh issue create output leads with the new issue`() {
        val tool = shell(
            "gh issue create -R beppe2880/opencode-mobile --title Open --body-file body.md",
            output = "\nCreating issue in beppe2880/opencode-mobile\n\nhttps://github.com/beppe2880/opencode-mobile/issues/157\n",
        )

        val links = GitHubLinks.forTool(tool)

        assertEquals(listOf(Kind.Issue to "157"), summary(links))
        assertEquals("https://github.com/beppe2880/opencode-mobile/issues/157", links.single().url)
        assertEquals("beppe2880", links.single().owner)
        assertEquals("opencode-mobile", links.single().repo)
    }

    @Test
    fun `gh pr create output gives a pull request`() {
        val tool = shell("gh pr create --fill", output = "Warning: 1 uncommitted change\nhttps://github.com/o/r/pull/156\n")

        assertEquals(listOf(Kind.PullRequest to "156"), summary(GitHubLinks.forTool(tool)))
    }

    @Test
    fun `git push offers to open a new pull request and ignores the remote`() {
        val output = """
            remote:
            remote: Create a pull request for 'feat/github-open-links' on GitHub by visiting:
            remote:      https://github.com/beppe2880/opencode-mobile/pull/new/feat/github-open-links
            remote:
            To https://github.com/beppe2880/opencode-mobile.git
             * [new branch]      feat/github-open-links -> feat/github-open-links
        """.trimIndent()

        val links = GitHubLinks.forTool(shell("git push -u origin HEAD", output = output))

        assertEquals(listOf(Kind.NewPullRequest to "feat/github-open-links"), summary(links))
        assertEquals("https://github.com/beppe2880/opencode-mobile/pull/new/feat/github-open-links", links.single().url)
    }

    @Test
    fun `a run url in the command is known while the tool still runs`() {
        val tool = shell("gh run watch https://github.com/o/r/actions/runs/19234567890 --exit-status", status = ToolStatus.Running)

        assertEquals(listOf(Kind.Run to "19234567890"), summary(GitHubLinks.forTool(tool)))
    }

    @Test
    fun `a job url inside a run is the run`() {
        val links = GitHubLinks.find("see https://github.com/o/r/actions/runs/123/job/456 for logs")

        assertEquals(listOf(Kind.Run to "123"), summary(links))
    }

    @Test
    fun `streaming input is not read`() {
        val tool = shell("gh run watch https://github.com/o/r/actions/runs/19", status = ToolStatus.Streaming)

        assertTrue(GitHubLinks.forTool(tool).isEmpty())
    }

    @Test
    fun `commit urls show the short hash`() {
        val links = GitHubLinks.find("Pushed https://github.com/o/r/commit/0123456789ABCDEF0123456789abcdef01234567")

        assertEquals(listOf(Kind.Commit to "0123456"), summary(links))
    }

    @Test
    fun `compare and file urls show the repository name`() {
        val links = GitHubLinks.find(
            "https://github.com/o/repo-a/compare/main...feat https://github.com/o/repo-b/blob/main/README.md https://github.com/o/repo-c/tree/main/docs",
        )

        assertEquals(listOf(Kind.Compare to "repo-a", Kind.File to "repo-b", Kind.File to "repo-c"), summary(links))
    }

    @Test
    fun `the last issue or pull request printed leads and the others follow`() {
        val output = """
            https://github.com/o/r/blob/main/a.kt
            https://github.com/o/r/actions/runs/77
            https://github.com/o/r/issues/10
            https://github.com/o/r/pull/11
        """.trimIndent()

        val links = GitHubLinks.forTool(shell("./release.sh", output = output))

        assertEquals(listOf(Kind.PullRequest to "11", Kind.Issue to "10", Kind.Run to "77", Kind.File to "r"), summary(links))
    }

    @Test
    fun `the same page appears once`() {
        val output = "https://github.com/o/r/issues/5 and https://github.com/o/r/issues/5#issuecomment-1 and https://github.com/O/R/issues/5?x=1"

        assertEquals(listOf(Kind.Issue to "5"), summary(GitHubLinks.find(output).distinctBy { it.identity }))
        assertEquals(1, GitHubLinks.forTool(shell("true", output = output)).size)
    }

    @Test
    fun `a web fetch leads with the page it was given`() {
        val tool = Part.Tool(
            "c1",
            "webfetch",
            ToolStatus.Completed,
            input = """{"url":"https://github.com/o/r/issues/42"}""",
            output = "Related: https://github.com/o/r/pull/7",
        )

        assertEquals(listOf(Kind.Issue to "42", Kind.PullRequest to "7"), summary(GitHubLinks.forTool(tool)))
    }

    @Test
    fun `an mcp result finds html urls and skips the api`() {
        val tool = Part.Tool(
            "c1",
            "github_create_issue",
            ToolStatus.Completed,
            input = """{"owner":"o","repo":"r","title":"Bug"}""",
            output = "{" +
                """"url":"https://api.github.com/repos/o/r/issues/9","html_url":"https://github.com/o/r/issues/9",""" +
                """"user":{"html_url":"https://github.com/o"}}""",
        )

        assertEquals(listOf(Kind.Issue to "9"), summary(GitHubLinks.forTool(tool)))
    }

    @Test
    fun `other hosts and pages without a label are ignored`() {
        val text = """
            https://raw.githubusercontent.com/o/r/main/README.md
            https://api.github.com/repos/o/r/pulls/3
            https://gist.github.com/o/abc
            https://github.com.evil.example/o/r/issues/1
            https://gitlab.com/o/r/-/issues/1
            https://github.com/o/r
            https://github.com/o/r/issues
            https://github.com/o/r/issues/new
            https://github.com/o/r/pulls
            https://github.com/orgs/acme/projects/1
            https://github.com/o/r/commit/xyz
        """.trimIndent()

        assertTrue(GitHubLinks.find(text).isEmpty())
    }

    @Test
    fun `trailing punctuation and brackets are not part of the link`() {
        val text = "Created (https://github.com/o/r/issues/157). See [PR](https://github.com/o/r/pull/156), and **https://github.com/o/r/pull/155**: done"

        val links = GitHubLinks.find(text)

        assertEquals(
            listOf("https://github.com/o/r/issues/157", "https://github.com/o/r/pull/156", "https://github.com/o/r/pull/155"),
            links.map { it.url },
        )
    }

    @Test
    fun `json escapes in the input end the link`() {
        val tool = shell("gh issue view https://github.com/o/r/issues/12\\necho done")

        assertEquals(listOf("https://github.com/o/r/issues/12"), GitHubLinks.forTool(tool).map { it.url })
    }

    @Test
    fun `text without github links has none`() {
        assertTrue(GitHubLinks.find("plain output\nhttps://example.com/a").isEmpty())
        assertNull(GitHubLinks.forTools(listOf(shell("ls", output = "a b c"))))
    }

    @Test
    fun `a group leads with the newest tool and names its part`() {
        val tools = listOf(
            shell("gh issue create", output = "https://github.com/o/r/issues/1", callId = "first"),
            shell("git log", output = "https://github.com/o/r/commit/abcdef1234", callId = "second"),
            shell("gh pr create", output = "https://github.com/o/r/pull/2", callId = "third"),
        )

        val group = GitHubLinks.forTools(tools)

        assertEquals("third", group?.partId)
        assertEquals(listOf(Kind.PullRequest to "2", Kind.Issue to "1", Kind.Commit to "abcdef1"), summary(group!!.links))
    }
}
