@file:Suppress("ktlint:standard:max-line-length") // sample transcripts and replies

package io.github.beppe2880.opencodemobile.fake

import io.github.beppe2880.opencodemobile.core.domain.FileDiff
import io.github.beppe2880.opencodemobile.core.domain.Form
import io.github.beppe2880.opencodemobile.core.domain.FormAnswer
import io.github.beppe2880.opencodemobile.core.domain.FormField
import io.github.beppe2880.opencodemobile.core.domain.FormOption
import io.github.beppe2880.opencodemobile.core.domain.Message
import io.github.beppe2880.opencodemobile.core.domain.PermissionDecision
import io.github.beppe2880.opencodemobile.core.domain.PermissionRequest
import io.github.beppe2880.opencodemobile.core.domain.ServerEvent
import io.github.beppe2880.opencodemobile.core.domain.SessionOutcome
import io.github.beppe2880.opencodemobile.core.domain.TokenUsage
import io.github.beppe2880.opencodemobile.core.domain.TranscriptReducer
import io.github.beppe2880.opencodemobile.core.ui.sample.SampleData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Scripted run of the "Tidy up shell aliases" session for demo recordings: it waits on a
 * permission, then edits files, asks a question, asks a second permission and finishes.
 * The script restarts with the app process.
 */
internal class DotfilesDemo(private val store: FakeStore, private val events: FakeEvents, private val scope: CoroutineScope) {
    private val answered = mutableSetOf<String>()

    /** Handles replies that belong to the script; false for anything else. */
    fun onPermission(requestId: String, decision: PermissionDecision): Boolean {
        if (requestId != BACKUP && requestId != RELOAD) return false
        if (requestId in answered) return true
        answered += requestId
        store.permissions[SESSION] = store.permissions[SESSION].orEmpty().filterNot { it.id == requestId }
        scope.launch {
            events.emit(ServerEvent.PermissionChanged(SESSION))
            when {
                decision == PermissionDecision.Reject ->
                    if (requestId == BACKUP) rejected("a_1", "c_backup") else rejected("a_2", "c_reload")
                requestId == BACKUP -> afterBackup()
                else -> afterReload()
            }
        }
        return true
    }

    fun onForm(formId: String, answers: Map<String, FormAnswer>): Boolean {
        if (formId != FORM) return false
        if (formId in answered) return true
        answered += formId
        store.forms[SESSION] = store.forms[SESSION].orEmpty().filterNot { it.id == formId }
        scope.launch {
            events.emit(ServerEvent.FormsChanged(SESSION))
            afterAnswer(answers)
        }
        return true
    }

    private suspend fun afterBackup() {
        send(ServerEvent.ToolCalled(SESSION, "a_1", "c_backup", BACKUP_INPUT))
        delay(500)
        send(ServerEvent.ToolSucceeded(SESSION, "a_1", "c_backup", "", emptyMap(), now()))
        send(ServerEvent.StepEnded(SESSION, "a_1", "tool-calls", 0.0118, TokenUsage(input = 2_400, output = 310, cacheRead = 18_200), now()))
        send(ServerEvent.StepStarted(SESSION, "a_2", AGENT, SampleData.sonnet, now()))
        reasoning("a_2", 0, "Backup is in place. I'll merge the navigation aliases onto one line, move the git ones to `git switch` and use a modern ls for listings.")
        tool("a_2", "c_edit1", "edit", editInput(FILE, "alias gco='git checkout'", "alias gco='git switch'"), "Edit applied successfully.", 1_100, listOf(FakeSamples.aliases))
        text("a_2", 1, "Grouped the navigation and git aliases and removed the duplicates. One choice is up to you:")
        delay(500)
        store.forms[SESSION] = listOf(form)
        events.emit(ServerEvent.FormsChanged(SESSION))
    }

    private suspend fun afterAnswer(answers: Map<String, FormAnswer>) {
        val lister = (answers["ls"] as? FormAnswer.Text)?.value ?: "eza"
        val git = true
        val listing = LISTERS[lister] ?: "ls -la --color=auto"
        delay(300)
        reasoning("a_2", 2, "Using `$lister` for listings${if (git) " and adding the git shortcuts" else ""}. Then I'll point .zshrc at the new file.")
        val lines = buildList {
            add("-alias ll='eza -la --git'")
            add("+alias ll='$listing'")
            add("+alias la='${listing.replace("-la", "-a")}'")
            if (git) {
                add("+alias gs='git status -sb'")
                add("+alias gp='git push'")
                add("+alias gl='git pull --rebase'")
            }
        }
        val shortcuts = FakeSamples.diff(
            ".config/zsh/aliases.zsh", "modified",
            "@@ -10,1 +10,${lines.count {
                it.startsWith("+")
            }} @@\n" + lines.joinToString("\n"),
        )
        tool("a_2", "c_edit2", "edit", editInput(FILE, "alias ll='eza -la --git'", "alias ll='$listing'"), "Edit applied successfully.", 900, listOf(shortcuts))
        tool("a_2", "c_edit3", "edit", editInput("/home/user/.zshrc", "source ~/.aliases", "source ~/.config/zsh/aliases.zsh"), "Edit applied successfully.", 700, listOf(FakeSamples.zshrc))
        text("a_2", 3, "Now I'll reload the shell to check that everything loads.")
        send(ServerEvent.ToolInputStarted(SESSION, "a_2", "c_reload", "bash", now()))
        send(ServerEvent.ToolInputEnded(SESSION, "a_2", "c_reload", RELOAD_INPUT))
        delay(400)
        store.permissions[SESSION] = listOf(PermissionRequest(RELOAD, SESSION, "bash", listOf(RELOAD_COMMAND), "Run a shell command", canSave = true))
        events.emit(ServerEvent.PermissionChanged(SESSION))
        summary = buildString {
            append("Done, your aliases are tidy:\n\n")
            append("- **Navigation:** `..`, `...` and `....` on one line\n")
            append("- **Git:** `gco` and `gcb` use `git switch`")
            append(if (git) ", plus `gs`, `gp` and `gl`\n" else "\n")
            append("- **Listing:** `ll` and `la` now use **$lister**\n")
            append("- `.zshrc` sources the new file, and all 27 aliases load without errors\n\n")
            append("The old version is in `aliases.zsh.bak` if you want to compare.")
        }
    }

    private var summary = ""

    private suspend fun afterReload() {
        send(ServerEvent.ToolCalled(SESSION, "a_2", "c_reload", RELOAD_INPUT))
        delay(700)
        send(ServerEvent.ToolSucceeded(SESSION, "a_2", "c_reload", "27", emptyMap(), now()))
        send(ServerEvent.StepEnded(SESSION, "a_2", "tool-calls", 0.0214, TokenUsage(input = 3_100, output = 820, reasoning = 140, cacheRead = 21_600), now()))
        send(ServerEvent.StepStarted(SESSION, "a_3", AGENT, SampleData.sonnet, now()))
        text("a_3", 0, summary)
        finish("a_3")
    }

    private suspend fun rejected(messageId: String, callId: String) {
        send(ServerEvent.ToolFailed(SESSION, messageId, callId, "The user rejected this command", now()))
        text(messageId, 9, "Ok, I won't run it. Tell me how you'd like to continue.")
        finish(messageId)
    }

    private suspend fun finish(messageId: String) {
        send(ServerEvent.StepEnded(SESSION, messageId, "stop", 0.0096, TokenUsage(input = 1_900, output = 260, cacheRead = 24_400), now()))
        store.running -= SESSION
        events.emit(ServerEvent.ExecutionEnded(SESSION, SessionOutcome.Succeeded))
    }

    private suspend fun reasoning(messageId: String, ordinal: Int, text: String) {
        val start = now()
        send(ServerEvent.ReasoningStarted(SESSION, messageId, ordinal, start))
        text.chunked(5).forEach {
            delay(28)
            send(ServerEvent.ReasoningDelta(SESSION, messageId, ordinal, it))
        }
        send(ServerEvent.ReasoningEnded(SESSION, messageId, ordinal, text, now()))
    }

    private suspend fun text(messageId: String, ordinal: Int, text: String) {
        send(ServerEvent.TextStarted(SESSION, messageId, ordinal))
        text.chunked(6).forEach {
            delay(32)
            send(ServerEvent.TextDelta(SESSION, messageId, ordinal, it))
        }
        send(ServerEvent.TextEnded(SESSION, messageId, ordinal, text))
    }

    private suspend fun tool(messageId: String, callId: String, name: String, input: String, output: String, runMs: Long, diffs: List<FileDiff> = emptyList()) {
        send(ServerEvent.ToolInputStarted(SESSION, messageId, callId, name, now()))
        send(ServerEvent.ToolCalled(SESSION, messageId, callId, input))
        delay(runMs)
        send(ServerEvent.ToolSucceeded(SESSION, messageId, callId, output, emptyMap(), now(), diffs))
    }

    private suspend fun send(event: ServerEvent.SessionScoped) {
        store.transcripts[SESSION] = TranscriptReducer.reduce(store.transcripts[SESSION].orEmpty(), event)
        events.emit(event)
    }

    companion object {
        const val SESSION = "ses_2"
        private const val AGENT = "build"
        private const val BACKUP = "per_backup"
        private const val RELOAD = "per_reload"
        private const val FORM = "form_aliases"
        private const val FILE = "/home/user/.config/zsh/aliases.zsh"
        private const val BACKUP_COMMAND = "cp ~/.config/zsh/aliases.zsh ~/.config/zsh/aliases.zsh.bak"
        private const val BACKUP_INPUT = """{"command":"$BACKUP_COMMAND","description":"Back up the aliases file"}"""
        private const val RELOAD_COMMAND = "source ~/.zshrc && alias | wc -l"
        private const val RELOAD_INPUT = """{"command":"$RELOAD_COMMAND","description":"Reload the shell config"}"""

        private val LISTERS = mapOf("eza" to "eza -la --git --icons", "lsd" to "lsd -la", "ls" to "ls -la --color=auto")

        private fun now() = System.currentTimeMillis()

        private fun editInput(path: String, old: String, new: String) =
            """{"filePath":"$path","oldString":"${old.replace("\"", "\\\"")}","newString":"${new.replace("\"", "\\\"")}"}"""

        private val form = Form(
            id = FORM,
            sessionId = SESSION,
            title = "Listing aliases",
            fields = listOf(
                FormField.Text(
                    key = "ls",
                    title = "Which tool should `ll` and `la` use?",
                    required = true,
                    options = listOf(
                        FormOption("eza", "eza", "Icons and git status per file"),
                        FormOption("lsd", "lsd", "Colors and icons, ls-compatible flags"),
                        FormOption("ls", "Plain ls", "Nothing extra to install"),
                    ),
                ),
            ),
        )

        val backupPermission = PermissionRequest(BACKUP, SESSION, "bash", listOf(BACKUP_COMMAND), "Run a shell command", canSave = true)

        /** The run so far: the prompt, a first look at the files and the backup waiting for approval. */
        fun transcript(): List<Message> {
            val t = now() - 50_000
            val user = Message.User("u_dotfiles", t, "Tidy up my shell aliases: group them, drop the duplicates and switch to modern tools where it makes sense.")
            val aliases = listOf(
                "# Navigation", "alias ..='cd ..'", "alias ...='cd ../..'", "", "# Git", "alias g='git'",
                "alias gco='git checkout'", "", "alias ll='ls -la'", "alias g='git'",
            ).mapIndexed { i, line -> "${i + 1}: $line" }.joinToString("\n")
            val steps = listOf(
                ServerEvent.StepStarted(SESSION, "a_1", AGENT, SampleData.sonnet, t + 1_000),
                ServerEvent.ReasoningStarted(SESSION, "a_1", 0, t + 1_200),
                ServerEvent.ReasoningEnded(SESSION, "a_1", 0, "I'll read the aliases first, look for duplicates across the dotfiles, and back the file up before touching it.", t + 3_400),
                ServerEvent.ToolInputStarted(SESSION, "a_1", "c_read", "read", t + 3_500),
                ServerEvent.ToolCalled(SESSION, "a_1", "c_read", """{"filePath":"$FILE"}"""),
                ServerEvent.ToolSucceeded(SESSION, "a_1", "c_read", aliases, emptyMap(), t + 3_900),
                ServerEvent.ToolInputStarted(SESSION, "a_1", "c_grep", "grep", t + 4_000),
                ServerEvent.ToolCalled(SESSION, "a_1", "c_grep", """{"pattern":"^alias ","path":"/home/user"}"""),
                ServerEvent.ToolSucceeded(SESSION, "a_1", "c_grep", "Found 11 matches in 2 files", emptyMap(), t + 4_600),
                ServerEvent.TextStarted(SESSION, "a_1", 1),
                ServerEvent.TextEnded(SESSION, "a_1", 1, "Your aliases are split between `~/.aliases` and `aliases.zsh`, `g` is defined twice and `gco` still uses `git checkout`. I'll back the file up before editing it."),
                ServerEvent.ToolInputStarted(SESSION, "a_1", "c_backup", "bash", t + 6_000),
                // Called only once approved, so it does not look like a running command meanwhile.
                ServerEvent.ToolInputEnded(SESSION, "a_1", "c_backup", BACKUP_INPUT),
            )
            return steps.fold(listOf<Message>(user)) { acc, event -> TranscriptReducer.reduce(acc, event) }
        }
    }
}
