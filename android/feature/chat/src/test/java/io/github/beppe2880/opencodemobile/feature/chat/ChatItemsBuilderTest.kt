package io.github.beppe2880.opencodemobile.feature.chat

import io.github.beppe2880.opencodemobile.core.domain.Message
import io.github.beppe2880.opencodemobile.core.domain.ModelInfo
import io.github.beppe2880.opencodemobile.core.domain.ModelRef
import io.github.beppe2880.opencodemobile.core.domain.NoticeKind
import io.github.beppe2880.opencodemobile.core.domain.Part
import io.github.beppe2880.opencodemobile.core.domain.SessionOutcome
import io.github.beppe2880.opencodemobile.core.domain.TokenUsage
import io.github.beppe2880.opencodemobile.core.domain.ToolStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatItemsBuilderTest {

    private val model = ModelRef("haiku", "anthropic")
    private val names: (ModelRef) -> ModelLabel = { ModelLabel("Claude Haiku", "Anthropic") }

    private fun tool(id: String, name: String, status: ToolStatus = ToolStatus.Completed) =
        Part.Tool(id, name, status, """{"filePath":"/a/$id"}""")

    private fun assistant(id: String, parts: List<Part>, created: Long, completed: Long? = created + 1_000, error: String? = null) =
        Message.Assistant(id, created, "build", model, parts, completedAt = completed, error = error, cost = 0.01)

    @Test
    fun `groups consecutive tools across steps and adds a footer`() {
        val messages = listOf(
            Message.User("u1", 0, "hi"),
            assistant("a1", listOf(tool("1", "read")), created = 100, completed = 500),
            assistant("a2", listOf(tool("2", "bash"), Part.Text(0, "done")), created = 600, completed = 3_000),
        )
        val items = ChatItemsBuilder.build(messages, running = false, showReasoning = true, modelLabel = names)

        assertEquals(listOf("u1", "tools-a1/1", "a2/text-0", "footer-a2"), items.map { it.key })
        val tools = items[1] as ChatItem.Tools
        assertEquals(listOf("read", "bash"), tools.tools.map { it.name })
        val footer = items.last() as ChatItem.Footer
        assertEquals(3_000L, footer.durationMs)
        assertEquals("Claude Haiku", footer.model)
        assertEquals("Anthropic", footer.provider)
        assertEquals("done", footer.copyText)
        assertEquals(0.02, footer.cost, 1e-9)
    }

    @Test
    fun `tool groups keep unique keys when call ids repeat across steps`() {
        val messages = listOf(
            Message.User("u1", 0, "hi"),
            assistant("a1", listOf(tool("call_0", "read"), Part.Text(0, "next")), created = 100),
            assistant("a2", listOf(tool("call_0", "bash"), Part.Text(0, "done")), created = 1_200),
        )
        val items = ChatItemsBuilder.build(messages, running = false, showReasoning = true, modelLabel = names)

        val keys = items.map { it.key }
        assertEquals(keys.distinct(), keys)
        assertEquals(listOf("tools-a1/call_0", "tools-a2/call_0"), items.filterIsInstance<ChatItem.Tools>().map { it.key })
    }

    @Test
    fun `thinking and tools with nothing in between collapse into one activity row`() {
        val messages = listOf(
            Message.User("u1", 0, "hi"),
            assistant("a1", listOf(Part.Reasoning(0, "plan", startedAt = 0, completedAt = 2_000), tool("1", "bash")), created = 100),
            assistant("a2", listOf(Part.Reasoning(0, "more", startedAt = 0, completedAt = 3_000), tool("2", "read"), tool("3", "edit")), created = 1_200),
            assistant("a3", listOf(Part.Text(0, "done"), Part.Reasoning(1, "alone", startedAt = 0, completedAt = 500)), created = 2_300),
        )

        val items = ChatItemsBuilder.build(messages, running = false, showReasoning = true, modelLabel = names, groupActivity = true)

        val activity = items[1] as ChatItem.Activity
        assertEquals(listOf("u1", activity.key, "a3/text-0", items[3].key, "footer-a3"), items.map { it.key })
        assertEquals(items.size, 5)
        assertEquals(listOf("bash", "read", "edit"), activity.tools.map { it.name })
        assertEquals(5_000L, activity.thinkingMs)
        assertEquals(4, activity.entries.size)
        // A single row is left as it is.
        assertTrue(items[3] is ChatItem.Reasoning)
    }

    @Test
    fun `without grouping thinking still splits the tool rows`() {
        val messages = listOf(
            Message.User("u1", 0, "hi"),
            assistant("a1", listOf(Part.Reasoning(0, "plan", startedAt = 0, completedAt = 2_000), tool("1", "bash")), created = 100),
            assistant("a2", listOf(Part.Reasoning(0, "more", startedAt = 0, completedAt = 3_000), tool("2", "read")), created = 1_200),
        )

        val items = ChatItemsBuilder.build(messages, running = false, showReasoning = true, modelLabel = names, groupActivity = false)

        assertEquals(0, items.count { it is ChatItem.Activity })
        assertEquals(2, items.count { it is ChatItem.Tools })
    }

    @Test
    fun `a running activity row reports progress while it grows`() {
        val messages = listOf(
            Message.User("u1", 0, "hi"),
            assistant("a1", listOf(Part.Reasoning(0, "plan", startedAt = 0, completedAt = 2_000), tool("1", "bash")), created = 100),
            assistant("a2", listOf(tool("2", "read", ToolStatus.Running)), created = 1_200, completed = null),
        )
        val before = ChatItemsBuilder.build(messages.take(2), running = true, showReasoning = true, modelLabel = names, groupActivity = true)
        val after = ChatItemsBuilder.build(messages, running = true, showReasoning = true, modelLabel = names, groupActivity = true)

        val activity = after[1] as ChatItem.Activity
        assertTrue(activity.running)
        assertEquals((before[1] as ChatItem.Activity).key, activity.key)
    }

    @Test
    fun `tokens per second matches the TUI and only counts streaming time`() {
        // Tools run between streamed and completed (1_000..3_000) and are not counted.
        val step1 = assistant("a1", emptyList(), created = 0, completed = 3_000)
            .copy(streamedAt = 1_000, tokens = TokenUsage(input = 5_000, output = 30, reasoning = 10))
        val step2 = assistant("a2", emptyList(), created = 4_000, completed = 5_100)
            .copy(streamedAt = 5_000, tokens = TokenUsage(output = 60))

        assertEquals(50.0, ChatItemsBuilder.tokensPerSecond(listOf(step1, step2))!!, 1e-9)
    }

    @Test
    fun `tokens per second is unknown until every step has streamed`() {
        val streamed = assistant("a1", emptyList(), created = 0).copy(streamedAt = 1_000, tokens = TokenUsage(output = 5))
        val notStreamed = assistant("a2", emptyList(), created = 2_000).copy(tokens = TokenUsage(output = 5))
        val noTokens = assistant("a3", emptyList(), created = 0).copy(streamedAt = 2_000)

        assertEquals(null, ChatItemsBuilder.tokensPerSecond(listOf(streamed, notStreamed)))
        assertEquals(null, ChatItemsBuilder.tokensPerSecond(listOf(noTokens)))
        assertEquals(null, ChatItemsBuilder.tokensPerSecond(emptyList()))
    }

    @Test
    fun `prompts steered during one execution share a single footer like the TUI`() {
        val messages = listOf(
            Message.User("u1", 0, "fix the rate"),
            assistant("a1", listOf(Part.Text(0, "working")), created = 100, completed = 60_000)
                .copy(streamedAt = 50_000, tokens = TokenUsage(input = 20, output = 500, cacheRead = 30_000, cacheWrite = 1_000)),
            Message.User("u2", 30_000, "also show diffs"),
            assistant("a2", listOf(Part.Text(0, "done")), created = 60_000, completed = 637_000)
                .copy(streamedAt = 110_000, tokens = TokenUsage(output = 400)),
            Message.Idle("i1", 637_100, SessionOutcome.Succeeded),
        )

        val items = ChatItemsBuilder.build(messages, running = false, showReasoning = true, modelLabel = names)

        assertEquals(listOf("u1", "a1/text-0", "u2", "a2/text-0", "footer-a2"), items.map { it.key })
        val footer = items.last() as ChatItem.Footer
        // From the first prompt of the execution to the end of its last step.
        assertEquals(637_000L, footer.durationMs)
        assertEquals(900 * 1_000.0 / (49_900 + 50_000), footer.tokensPerSecond!!, 1e-9)
        assertEquals("working\n\ndone", footer.copyText)
        // New tokens of every step of the execution; re-read cache (30k) is not counted.
        assertEquals(1_520L + 400L, footer.totalTokens)
    }

    @Test
    fun `executions are separated by idle markers and idle errors are shown`() {
        val messages = listOf(
            Message.User("u1", 0, "one"),
            assistant("a1", listOf(Part.Text(0, "first")), created = 10, completed = 1_000),
            Message.Idle("i1", 1_001, SessionOutcome.Succeeded),
            Message.User("u2", 2_000, "two"),
            assistant("a2", listOf(Part.Text(0, "second")), created = 2_010, completed = 2_500, error = "Step interrupted"),
            Message.Idle("i2", 2_600, SessionOutcome.Interrupted, error = "Rate limited"),
        )

        val items = ChatItemsBuilder.build(messages, running = false, showReasoning = true, modelLabel = names)

        assertEquals(
            listOf("u1", "a1/text-0", "footer-a1", "u2", "a2/text-0", "a2/error", "footer-a2", "i2"),
            items.map { it.key },
        )
        assertEquals(1_000L, (items[2] as ChatItem.Footer).durationMs)
        assertEquals(500L, (items[6] as ChatItem.Footer).durationMs)
        assertTrue((items[6] as ChatItem.Footer).interrupted)
        assertEquals(ChatItem.Notice("i2", NoticeKind.Error, "Rate limited"), items.last())
    }

    @Test
    fun `sessions without idle markers start a turn at every prompt`() {
        val messages = listOf(
            Message.User("u1", 0, "one"),
            assistant("a1", listOf(Part.Text(0, "first")), created = 10, completed = 1_000),
            Message.User("u2", 2_000, "two"),
            assistant("a2", listOf(Part.Text(0, "second")), created = 2_010, completed = 2_500),
        )

        val items = ChatItemsBuilder.build(messages, running = false, showReasoning = true, modelLabel = names)

        assertEquals(listOf("u1", "a1/text-0", "footer-a1", "u2", "a2/text-0", "footer-a2"), items.map { it.key })
    }

    @Test
    fun `the running execution has no footer yet`() {
        val messages = listOf(
            Message.Idle("i0", 0, SessionOutcome.Succeeded),
            Message.User("u1", 10, "go"),
            assistant("a1", listOf(Part.Text(0, "step")), created = 20, completed = 500),
            Message.User("u2", 600, "steer", pending = true),
        )

        val items = ChatItemsBuilder.build(messages, running = true, showReasoning = true, modelLabel = names)

        assertEquals(listOf("u1", "a1/text-0", "u2", "working"), items.map { it.key })
    }

    @Test
    fun `unknown models fall back to raw ids`() {
        assertEquals(ModelLabel("claude-x", "bedrock"), emptyList<ModelInfo>().labelFor(ModelRef("claude-x", "bedrock")))
    }

    @Test
    fun `running turn shows working row instead of footer`() {
        val messages = listOf(
            Message.User("u1", 0, "hi"),
            assistant("a1", listOf(Part.Text(0, "partial")), created = 100, completed = 200),
        )
        val items = ChatItemsBuilder.build(messages, running = true, showReasoning = true, modelLabel = names)

        assertEquals(listOf("u1", "a1/text-0", "working"), items.map { it.key })
    }

    @Test
    fun `streaming text suppresses the working row`() {
        val messages = listOf(
            Message.User("u1", 0, "hi"),
            assistant("a1", listOf(Part.Text(0, "par", streaming = true)), created = 100, completed = null),
        )
        val items = ChatItemsBuilder.build(messages, running = true, showReasoning = true, modelLabel = names)

        assertEquals(listOf("u1", "a1/text-0"), items.map { it.key })
    }

    @Test
    fun `reasoning is hidden when disabled and timed when shown`() {
        val reasoning = Part.Reasoning(0, "think", startedAt = 10, completedAt = 2_010)
        val messages = listOf(assistant("a1", listOf(reasoning, Part.Text(1, "ok")), created = 0))

        val hidden = ChatItemsBuilder.build(messages, running = false, showReasoning = false, modelLabel = names)
        assertTrue(hidden.none { it is ChatItem.Reasoning })

        val shown = ChatItemsBuilder.build(messages, running = false, showReasoning = true, modelLabel = names)
        assertEquals(2_000L, (shown.first() as ChatItem.Reasoning).durationMs)
    }

    @Test
    fun `errors and notices close tool groups and turns`() {
        val messages = listOf(
            Message.User("u1", 0, "hi"),
            assistant("a1", listOf(tool("1", "edit", ToolStatus.Error)), created = 10, error = "boom"),
            Message.Notice("n1", 2_000, NoticeKind.ModelSwitched, "Switched"),
        )
        val items = ChatItemsBuilder.build(messages, running = false, showReasoning = true, modelLabel = names)

        assertEquals(listOf("u1", "tools-a1/1", "a1/error", "footer-a1", "n1"), items.map { it.key })
        assertTrue((items[3] as ChatItem.Footer).interrupted)
        assertEquals(1, (items[1] as ChatItem.Tools).failed)
    }

    @Test
    fun `tool primary argument handles partial and escaped json`() {
        assertEquals("echo \"hi\"", ToolInfo.primaryArgument("""{"command":"echo \"hi\"","description":"x"}"""))
        assertEquals("/src/Ma", ToolInfo.primaryArgument("""{"filePath":"/src/Ma"""))
        assertEquals(null, ToolInfo.primaryArgument("{}"))
        assertEquals("Shell", ToolInfo.title("bash"))
    }
}
