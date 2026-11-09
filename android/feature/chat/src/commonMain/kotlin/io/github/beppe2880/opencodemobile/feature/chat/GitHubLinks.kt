package io.github.beppe2880.opencodemobile.feature.chat

import io.github.beppe2880.opencodemobile.core.domain.Part
import io.github.beppe2880.opencodemobile.core.domain.ToolStatus

/**
 * A github.com page a tool printed or worked on. [ref] is what its label shows: the issue or
 * pull request number, the run id, the short commit hash, the branch of a new pull request,
 * or the repository name for compare views and files.
 */
data class GitHubLink(
    val url: String,
    val kind: Kind,
    val owner: String,
    val repo: String,
    val ref: String,
) {
    /** [priority]: lower kinds lead the row when a tool printed several links. */
    enum class Kind(internal val priority: Int) {
        Issue(0),
        PullRequest(0),
        NewPullRequest(0),
        Run(1),
        Commit(1),
        Compare(2),
        File(3),
    }

    /** One entry per page: comment anchors or query strings of the same issue are the same link. */
    internal val identity: String get() = "${owner.lowercase()}/${repo.lowercase()}/$kind/$ref"
}

/** The GitHub links of a group of tools; [partId] is the call whose link leads. */
data class ToolGitHubLinks(val partId: String, val links: List<GitHubLink>)

/**
 * Finds GitHub issues, pull requests, workflow runs, commits, compare views and files in tool
 * calls (shell commands and output, web fetches and searches, MCP results). Only github.com
 * pages: raw.githubusercontent.com, api.github.com and other hosts are ignored.
 */
object GitHubLinks {

    /** Links offered by one row: the button plus a short menu. */
    private const val MAX_LINKS = 8

    // Stops at whitespace, quotes, brackets and backslashes (JSON escapes such as \n in tool input).
    private val URL = Regex("""https?://(?:www\.)?github\.com/[^\s"'<>()\[\]{}\\`|^]+""")
    private val TRAILING = charArrayOf('.', ',', ';', ':', '!', '?', '*', '_', '~')
    private val NAME = Regex("""[A-Za-z0-9_.-]+""")
    private val NUMBER = Regex("""[0-9]+""")
    private val SHA = Regex("""[0-9a-fA-F]{7,40}""")

    /** github.com paths that are not repositories. */
    private val RESERVED_OWNERS = setOf(
        "orgs", "settings", "notifications", "login", "logout", "marketplace", "features", "apps",
        "sponsors", "topics", "search", "users", "about", "pricing", "enterprise", "collections",
        "trending", "explore", "new", "organizations", "codespaces", "pulls", "issues",
    )

    /** Every GitHub link in [text], in the order they appear. */
    fun find(text: String): List<GitHubLink> {
        if ("github.com/" !in text) return emptyList()
        return URL.findAll(text).mapNotNull { classify(it.value.trimEnd(*TRAILING)) }.toList()
    }

    /**
     * The links of one tool call, most relevant first. A shell prints what it just created
     * last (`gh issue create`, `gh pr create`, `git push`), so its output leads, newest first,
     * then the command. Other tools (web fetch, MCP) lead with the page they were given.
     * Issues and pull requests come before runs and commits, which come before files.
     */
    fun forTool(tool: Part.Tool): List<GitHubLink> {
        // Streamed input is cut anywhere: a half-written number would open the wrong page.
        val input = if (tool.status == ToolStatus.Streaming) emptyList() else find(tool.input)
        val output = find(listOfNotNull(tool.output, tool.error).joinToString("\n"))
        val ordered = if (ToolInfo.kind(tool.name) == ToolInfo.Kind.Shell) {
            output.asReversed() + input.asReversed()
        } else {
            input + output
        }
        return rank(ordered)
    }

    /** The links of a group of tools for its summary row; the newest tool's links lead. */
    fun forTools(tools: List<Part.Tool>): ToolGitHubLinks? {
        val found = tools.asReversed().flatMap { tool -> forTool(tool).map { tool.callId to it } }
        val ranked = found.distinctBy { it.second.identity }.sortedBy { it.second.kind.priority }
        val lead = ranked.firstOrNull() ?: return null
        return ToolGitHubLinks(lead.first, ranked.take(MAX_LINKS).map { it.second })
    }

    private fun rank(links: List<GitHubLink>): List<GitHubLink> =
        links.distinctBy { it.identity }.sortedBy { it.kind.priority }.take(MAX_LINKS)

    /** What a github.com URL points to, or null for pages without a label (profiles, repositories, lists). */
    internal fun classify(url: String): GitHubLink? {
        val path = url.substringAfter("github.com/").substringBefore('#').substringBefore('?')
        val segments = path.split('/').filter { it.isNotEmpty() }
        if (segments.size < 4) return null
        val owner = segments[0]
        val repo = segments[1].removeSuffix(".git")
        if (!NAME.matches(owner) || !NAME.matches(repo) || owner.lowercase() in RESERVED_OWNERS) return null
        val section = segments[2]
        val target = segments[3]
        fun link(kind: GitHubLink.Kind, ref: String) = GitHubLink(url, kind, owner, repo, ref)
        return when (section) {
            "issues" -> if (NUMBER.matches(target)) link(GitHubLink.Kind.Issue, target) else null
            "pull" -> when {
                NUMBER.matches(target) -> link(GitHubLink.Kind.PullRequest, target)
                target == "new" && segments.size > 4 -> link(GitHubLink.Kind.NewPullRequest, segments.drop(4).joinToString("/"))
                else -> null
            }
            "actions" -> {
                val run = segments.getOrNull(4)
                if (target == "runs" && run != null && NUMBER.matches(run)) link(GitHubLink.Kind.Run, run) else null
            }
            "commit" -> if (SHA.matches(target)) link(GitHubLink.Kind.Commit, target.take(7).lowercase()) else null
            "compare" -> link(GitHubLink.Kind.Compare, repo)
            "blob", "tree" -> link(GitHubLink.Kind.File, repo)
            else -> null
        }
    }
}
