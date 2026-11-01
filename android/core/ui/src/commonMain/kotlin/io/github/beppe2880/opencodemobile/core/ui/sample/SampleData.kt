@file:Suppress("ktlint:standard:max-line-length") // sample transcripts and replies

package io.github.beppe2880.opencodemobile.core.ui.sample

import io.github.beppe2880.opencodemobile.core.domain.Agent
import io.github.beppe2880.opencodemobile.core.domain.AgentMode
import io.github.beppe2880.opencodemobile.core.domain.CredentialKind
import io.github.beppe2880.opencodemobile.core.domain.FileDiff
import io.github.beppe2880.opencodemobile.core.domain.Form
import io.github.beppe2880.opencodemobile.core.domain.FormField
import io.github.beppe2880.opencodemobile.core.domain.FormOption
import io.github.beppe2880.opencodemobile.core.domain.Message
import io.github.beppe2880.opencodemobile.core.domain.ModelInfo
import io.github.beppe2880.opencodemobile.core.domain.ModelRef
import io.github.beppe2880.opencodemobile.core.domain.NoticeKind
import io.github.beppe2880.opencodemobile.core.domain.Part
import io.github.beppe2880.opencodemobile.core.domain.PermissionRequest
import io.github.beppe2880.opencodemobile.core.domain.Project
import io.github.beppe2880.opencodemobile.core.domain.RunningAgent
import io.github.beppe2880.opencodemobile.core.domain.RunningCommand
import io.github.beppe2880.opencodemobile.core.domain.RunningWork
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

    private fun session(
        id: String,
        title: String,
        dir: String,
        ago: Long,
        outcome: SessionOutcome? = SessionOutcome.Succeeded,
        agent: String = "build",
    ) = Session(
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

    /**
     * One short turn with something for every chat display setting: two thoughts in a row, a
     * shell command with a long output, a read, the answer and its cost (the chat customizer).
     */
    val customizeTranscript: List<Message> = listOf(
        Message.User("msg_c_u1", NOW - 4 * MINUTE, "Run the cart tests and fix whatever fails."),
        Message.Assistant(
            id = "msg_c_a1",
            createdAt = NOW - 4 * MINUTE + 300,
            agent = "build",
            model = haiku,
            parts = listOf(
                Part.Reasoning(0, "The user wants the cart tests to pass. I'll run them first to see what fails.", startedAt = NOW - 4 * MINUTE + 400, completedAt = NOW - 4 * MINUTE + 2_400),
                Part.Reasoning(1, "Only the coupon tests run in the cart module, so a module-wide run is quick.", startedAt = NOW - 4 * MINUTE + 2_400, completedAt = NOW - 4 * MINUTE + 3_400),
                Part.Tool(
                    "call_c1",
                    "bash",
                    ToolStatus.Completed,
                    """{"command":"./gradlew :cart:test --tests 'shop.cart.CouponTest' --console=plain","description":"Run the cart tests"}""",
                    output = (1..14).joinToString("\n") {
                        "CouponTest > case $it PASSED"
                    } + "\nCouponTest > rounds the discount down FAILED\n\n15 tests completed, 1 failed",
                    startedAt = NOW - 4 * MINUTE + 3_500,
                    completedAt = NOW - 4 * MINUTE + 9_500,
                ),
                Part.Tool("call_c2", "read", ToolStatus.Completed, """{"filePath":"/home/user/code/shop/src/cart/Coupon.kt"}""", output = "15:     fun apply(subtotal: Long): Long = subtotal - subtotal * percent / 100", startedAt = NOW - 4 * MINUTE + 9_600, completedAt = NOW - 4 * MINUTE + 9_700),
                Part.Text(2, "One test fails: `Coupon.apply` rounds the discount up instead of down. I'll fix it, clear the build folder and run the tests again."),
            ),
            completedAt = NOW - 4 * MINUTE + 14_000,
            finish = "stop",
            cost = 0.0042,
            tokens = TokenUsage(input = 11_000, output = 260),
        ),
    )

    /** [customizeTranscript] while the agent is still at work: thinking done, the tests running. */
    val customizeRunning: List<Message> = customizeTranscript.map { message ->
        if (message !is Message.Assistant) return@map message
        message.copy(
            parts = message.parts.take(3).map { part ->
                if (part is Part.Tool) part.copy(status = ToolStatus.Running, output = part.output?.lines()?.take(5)?.joinToString("\n"), completedAt = null) else part
            },
            completedAt = null,
            finish = null,
        )
    }

    /** A question from the agent, for the question card. */
    val question = Form(
        id = "form_1",
        sessionId = "ses_3",
        title = "Checkout redesign",
        fields = listOf(
            FormField.Text(
                key = "layout",
                title = "Which checkout layout should I plan for?",
                required = true,
                options = listOf(
                    FormOption("single", "Single page", "Address, shipping and payment on one screen"),
                    FormOption("steps", "Three steps", "Separate address, shipping and payment steps"),
                ),
                custom = true,
            ),
        ),
    )

    /** Work running in the background of a session: a subagent and a dev server. */
    val runningWork = RunningWork(
        agents = listOf(RunningAgent(sessions[2].copy(title = "Explore the cart module", agent = "explore"), NOW - 2 * MINUTE)),
        commands = listOf(RunningCommand("cmd_1", "npm run dev", "/home/user/code/shop", NOW - 6 * MINUTE)),
    )

    /**
     * One step of the agent at work, for each kind of running work the chat can follow: [kind]
     * is the tool kind (or thinking); [lines] of shell output have arrived so far.
     */
    fun liveStep(kind: LiveKind, lines: Int = 3): List<Message> {
        val start = NOW - MINUTE
        val part: Part = when (kind) {
            LiveKind.Thinking -> Part.Reasoning(0, "The coupon test expects 10% off 999 to give 900. Integer division rounds the discount down, so the bug is elsewhere…", streaming = true, startedAt = start)
            LiveKind.Shell -> Part.Tool(
                "call_l1",
                "bash",
                ToolStatus.Running,
                """{"command":"./gradlew :cart:test","description":"Run the cart tests"}""",
                output = (1..lines).joinToString("\n") { "CouponTest > case $it PASSED" },
                startedAt = start,
            )
            LiveKind.Edit -> Part.Tool(
                "call_l2",
                "edit",
                ToolStatus.Completed,
                """{"filePath":"/home/user/code/shop/src/cart/Coupon.kt"}""",
                output = "Edit applied",
                startedAt = start,
                completedAt = start + 400,
                diffs = listOf(
                    FileDiff(
                        "src/cart/Coupon.kt",
                        "modified",
                        1,
                        1,
                        "@@ -15,1 +15,1 @@\n-    fun apply(subtotal: Long): Long = subtotal - (subtotal * percent + 99) / 100\n+    fun apply(subtotal: Long): Long = subtotal - subtotal * percent / 100",
                    ),
                ),
            )
            LiveKind.Read -> Part.Tool("call_l3", "grep", ToolStatus.Completed, """{"pattern":"fun apply","path":"src/cart"}""", output = "src/cart/Coupon.kt:15: fun apply(subtotal: Long)", startedAt = start, completedAt = start + 200)
            LiveKind.Other -> Part.Tool("call_l4", "webfetch", ToolStatus.Completed, """{"url":"https://kotlinlang.org/docs/numbers.html"}""", output = "Integer division always rounds toward zero.", startedAt = start, completedAt = start + 900)
        }
        return listOf(
            Message.User("msg_l_u_${kind.name}", start - 500, "Fix the coupon rounding."),
            Message.Assistant("msg_l_a_${kind.name}", start, "build", haiku, listOf(part)),
        )
    }

    enum class LiveKind { Thinking, Shell, Edit, Read, Other }

    val permission = PermissionRequest(
        id = "per_1",
        sessionId = "ses_1",
        action = "bash",
        resources = listOf("rm -rf build/"),
        message = "Run a shell command",
        canSave = true,
    )
}
