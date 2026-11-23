package io.github.beppe2880.opencodemobile.feature.chat

import io.github.beppe2880.opencodemobile.core.domain.OpenCodeError
import io.github.beppe2880.opencodemobile.core.ui.text.UiText
import io.github.beppe2880.opencodemobile.core.ui.text.toUiText
import io.github.beppe2880.opencodemobile.feature.chat.resources.Res
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_btw_failed
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_btw_unavailable
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * App-lifetime side questions, independent of chat ViewModels. Switching chats keeps requests
 * alive; closing this owner cancels them. Nothing is saved on disk. Keep the latest 20 settled
 * sessions, never evict an in-flight request, and fail stalled requests after five minutes.
 */
class BtwMemory(parentScope: CoroutineScope) {
    private val job = SupervisorJob(parentScope.coroutineContext[Job])
    private val scope = CoroutineScope(parentScope.coroutineContext + job)
    private val lock = SynchronizedObject()
    private val answers = MutableStateFlow<Map<Pair<String, String>, BtwState>>(emptyMap())

    fun get(serverId: String, sessionId: String): BtwState? = answers.value[serverId to sessionId]

    fun observe(serverId: String, sessionId: String): Flow<BtwState?> =
        answers.map { it[serverId to sessionId] }.distinctUntilChanged()

    fun put(serverId: String, sessionId: String, state: BtwState) {
        if (state.loading) return
        synchronized(lock) {
            val key = serverId to sessionId
            if (answers.value[key]?.loading != true) put(key, state)
        }
    }

    /** Returns false when this session already has a request or the app owner has closed. */
    fun ask(serverId: String, sessionId: String, question: String, generate: suspend () -> Result<String>): Boolean {
        val key = serverId to sessionId
        synchronized(lock) {
            if (!scope.isActive || question.isBlank() || answers.value[key]?.loading == true) return false
            put(key, BtwState(question = question, loading = true))
        }
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            val result = try {
                withTimeout(300_000) { generate() }.fold(
                    onSuccess = { BtwState(question = question, answer = it.trim()) },
                    onFailure = { error -> failed(question, error) },
                )
            } catch (error: TimeoutCancellationException) {
                BtwState(question = question, error = UiText.Resource(Res.string.chat_btw_failed))
            } catch (error: CancellationException) {
                synchronized(lock) { put(key, BtwState(question = question, error = UiText.Resource(Res.string.chat_btw_failed))) }
                throw error
            } catch (error: Exception) {
                failed(question, error)
            }
            coroutineContext.ensureActive()
            synchronized(lock) { put(key, result) }
        }
        return true
    }

    fun close() {
        job.cancel()
    }

    private fun failed(question: String, error: Throwable): BtwState = BtwState(
        question = question,
        error = if (error is OpenCodeError.NotFound) UiText.Resource(Res.string.chat_btw_unavailable) else error.toUiText(),
    )

    /** All writes are under [lock]; pending sessions may temporarily exceed the settled limit. */
    private fun put(key: Pair<String, String>, state: BtwState) {
        val next = LinkedHashMap(answers.value)
        next.remove(key)
        next[key] = state
        while (next.values.count { !it.loading } > 20) {
            val oldest = next.entries.firstOrNull { !it.value.loading }?.key ?: break
            next.remove(oldest)
        }
        answers.value = next
    }
}
