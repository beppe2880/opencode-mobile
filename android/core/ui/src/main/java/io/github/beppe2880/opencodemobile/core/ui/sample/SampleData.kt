package io.github.beppe2880.opencodemobile.core.ui.sample

import io.github.beppe2880.opencodemobile.core.domain.Agent
import io.github.beppe2880.opencodemobile.core.domain.AgentMode
import io.github.beppe2880.opencodemobile.core.domain.CredentialKind
import io.github.beppe2880.opencodemobile.core.domain.Message
import io.github.beppe2880.opencodemobile.core.domain.ModelInfo
import io.github.beppe2880.opencodemobile.core.domain.ModelRef
import io.github.beppe2880.opencodemobile.core.domain.NoticeKind
import io.github.beppe2880.opencodemobile.core.domain.Part
import io.github.beppe2880.opencodemobile.core.domain.PermissionRequest
import io.github.beppe2880.opencodemobile.core.domain.Project
import io.github.beppe2880.opencodemobile.core.domain.ServerProfile
import io.github.beppe2880.opencodemobile.core.domain.Session
import io.github.beppe2880.opencodemobile.core.domain.SessionOutcome
import io.github.beppe2880.opencodemobile.core.domain.TokenUsage
import io.github.beppe2880.opencodemobile.core.domain.ToolStatus

/** Realistic sample data for previews and the debug fake data layer. */
object SampleData {
    const val NOW = 1_760_356_800_000L // fixed for deterministic previews
    private const val MINUTE = 60_000L
    private const val HOUR = 60 * MINUTE
    private const val DAY = 24 * HOUR

    val haiku = ModelRef(id = "claude-haiku-5-5", providerId = "anthropic")
    val sonnet = ModelRef(id = "claude-sonnet-5", providerId = "anthropic")
    val gpt = ModelRef(id = "gpt-astra", providerId = "openai")

    val servers = listOf(
        ServerProfile("srv-desktop", "Desktop", "http://localhost:4096", secret = "x", credential = CredentialKind.PairingToken, addedAt = NOW - DAY),
        ServerProfile("srv-nas", "NAS", "http://192.168.1.20:4096", secret = "x", credential = CredentialKind.Password, addedAt = NOW - 5 * DAY),
    )

    val projects = listOf(
        Project("p-dotfiles", "/home/user/dotfiles", null, "git", NOW - HOUR),
        Project("p-mobile", "/home/user/code/opencode-mobile", null, "git", NOW - 2 * HOUR),
        Project("p-shop", "/home/user/code/shop", "shop", "git", NOW - DAY),
        Project("p-blog", "/home/user/code/blog", null, "git", NOW - 3 * DAY),
    )

    private fun session(id: String, title: String, dir: String, ago: Long, outcome: SessionOutcome? = SessionOutcome.Succeeded, agent: String = "build") = Session(
        id = id,
        parentId = null,
        projectId = "p",
        title = title,
        directory = dir,
        agent = agent,
        model = haiku,
        cost = 0.0123,
        tokens = TokenUsage(input = 12_000, output = 800),
        outcome = outcome,
        createdAt = NOW - ago - HOUR,
        updatedAt = NOW - ago,
    archivedAt = null,
    )

    val sessions = listOf(
        session("ses_1", "Native Android client for OpenCode", "/home/user/code/opencode-mobile", 2 * MINUTE),
        session("ses_2", "Tidy up shell aliases", "/home/user/dotfiles", 40 * MINUTE),
        session("ses_3", "Plan the checkout redesign", "/home/user/code/shop", 3 * HOUR, agent = "plan"),
        session("ses_4", "Fix flaky RSS feed test", "/home/user/code/blog", 5 * HOUR, outcome = SessionOutcome.Failed),
        session("ses_5", "Migrate config from opencode 1.0 to 2.0", "/", DAY + HOUR),
        session("ses_6", "Add coupon codes to the cart", "/home/user/code/shop", DAY + 4 * HOUR, outcome = SessionOutcome.Interrupted),
        session("ses_7", "Switch the blog theme to dark mode", "/home/user/code/blog", 3 * DAY),
        session("ses_8", "Summarize last week's pull requests", "/home/user/code/shop", 12 * DAY),
    )

    val agents = listOf(
        Agent("build", "build", "Default agent with all tools", AgentMode.Primary, hidden = false, color = null),
        Agent("plan", "plan", "Read-only planning agent", AgentMode.Primary, hidden = false, color = null),
        Agent("explore", "explore", "Fast codebase exploration", AgentMode.Subagent, hidden = false, color = null),
        Agent("title", "title", null, AgentMode.Primary, hidden = true, color = null),
    )

    val models = listOf(
        ModelInfo(haiku, "Claude Haiku 5.5", "anthropic", "claude", 200_000, true, emptyList(), NOW - 10 * DAY, true, "active", "Anthropic"),
        ModelInfo(sonnet, "Claude Sonnet 5", "anthropic", "claude", 1_000_000, true, listOf("high"), NOW - 30 * DAY, true, "active", "Anthropic"),
        ModelInfo(gpt, "GPT Astra", "openai", "gpt", 400_000, true, listOf("low", "medium", "high"), NOW - 20 * DAY, true, "active", "OpenAI"),
        ModelInfo(ModelRef("gemini-3-pro", "google"), "Gemini 3 Pro", "google", "gemini", 1_000_000, true, emptyList(), NOW - 40 * DAY, true, "active", "Google"),
    )

    val transcript: List<Message> = listOf(
        Message.User("msg_u1", NOW - 10 * MINUTE, "Read the file a.txt with the read tool, then run `echo probe-ok` with bash, then reply with one short sentence."),
        Message.Assistant(
            id = "msg_a1",
            createdAt = NOW - 10 * MINUTE + 500,
            agent = "build",
            model = haiku,
            parts = listOf(
                Part.Reasoning(0, "The user wants me to read a file and run a command. Let me do both.", startedAt = NOW - 10 * MINUTE + 600, completedAt = NOW - 10 * MINUTE + 2_600),
                Part.Tool("call_1", "read", ToolStatus.Completed, """{"filePath":"/home/user/code/opencode-mobile/a.txt"}""", output = "1: hello", startedAt = NOW - 10 * MINUTE + 2_700, completedAt = NOW - 10 * MINUTE + 2_800),
                Part.Tool("call_2", "bash", ToolStatus.Completed, """{"command":"echo probe-ok","description":"Print probe"}""", output = "probe-ok", startedAt = NOW - 10 * MINUTE + 2_900, completedAt = NOW - 10 * MINUTE + 3_100),
                Part.Text(1, "Read `a.txt` (it contains \"hello\") and ran `echo probe-ok`, which printed `probe-ok`."),
            ),
            completedAt = NOW - 10 * MINUTE + 3_500,
            finish = "stop",
            cost = 0.0031,
            tokens = TokenUsage(input = 9_000, output = 120),
        ),
        Message.Notice("msg_n1", NOW - 8 * MINUTE, NoticeKind.ModelSwitched, "Switched model to Claude Sonnet 5"),
        Message.User("msg_u2", NOW - 7 * MINUTE, "Show me a Kotlin data class for a session and explain it briefly."),
        Message.Assistant(
            id = "msg_a2",
            createdAt = NOW - 7 * MINUTE + 400,
            agent = "build",
            model = sonnet,
            parts = listOf(
                Part.Tool("call_3", "grep", ToolStatus.Completed, """{"pattern":"data class Session","path":"android/core/domain"}""", output = "Session.kt:42: data class Session(", startedAt = NOW - 7 * MINUTE + 500, completedAt = NOW - 7 * MINUTE + 700),
                Part.Tool("call_4", "edit", ToolStatus.Error, """{"filePath":"android/core/domain/Session.kt"}""", error = "File has been modified since it was last read", startedAt = NOW - 7 * MINUTE + 800, completedAt = NOW - 7 * MINUTE + 900),
                Part.Text(
                    0,
                    """
                    Here is the model used by the app:

                    ```kotlin
                    data class Session(
                        val id: String,
                        val title: String,
                        val directory: String,
                    )
                    ```

                    - **id**: server identifier
                    - **directory**: working directory of the agent
                    """.trimIndent(),
                ),
            ),
            completedAt = NOW - 7 * MINUTE + 6_000,
            finish = "stop",
            cost = 0.0182,
            tokens = TokenUsage(input = 14_000, output = 420),
        ),
    )

    val permission = PermissionRequest(
        id = "per_1",
        sessionId = "ses_1",
        action = "bash",
        resources = listOf("rm -rf build/"),
        message = "Run a shell command",
        canSave = true,
    )
}
