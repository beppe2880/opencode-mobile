package io.github.beppe2880.opencodemobile.feature.settings.usage

import io.github.beppe2880.opencodemobile.core.domain.Agent
import io.github.beppe2880.opencodemobile.core.domain.CatalogRepository
import io.github.beppe2880.opencodemobile.core.domain.ModelInfo
import io.github.beppe2880.opencodemobile.core.domain.ModelRef
import io.github.beppe2880.opencodemobile.core.domain.OpenCodeError
import io.github.beppe2880.opencodemobile.core.domain.Project
import io.github.beppe2880.opencodemobile.core.domain.Session
import io.github.beppe2880.opencodemobile.core.domain.SessionRepository
import io.github.beppe2880.opencodemobile.core.domain.TokenUsage
import io.github.beppe2880.opencodemobile.core.domain.UsageStats
import io.github.beppe2880.opencodemobile.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class UsageViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val rome = ZoneId.of("Europe/Rome")

    /** Sunday 18 October 2026, 15:30 in Rome. */
    private val now = ZonedDateTime.of(2026, 10, 18, 15, 30, 0, 0, rome).toInstant().toEpochMilli()

    private val sessions = FakeSessions()

    private fun vm() = UsageViewModel(sessions, FakeCatalog(), clock = { now }, zone = { rome })

    @Test
    fun `periods start at local midnight of the first day and end now`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        // 7 days by default: from Monday 12 October.
        assertEquals(Triple(midnight(12), now, "Europe/Rome"), sessions.requests.single())
        assertEquals(42.0, vm.state.value.stats?.cost)
        assertEquals(ModelName("Claude Haiku", "Anthropic"), vm.state.value.modelNames["anthropic/haiku"])

        vm.onEvent(UsageEvent.SelectPeriod(UsagePeriod.Today))
        advanceUntilIdle()
        assertEquals(midnight(18), sessions.requests.last().first)

        vm.onEvent(UsageEvent.SelectPeriod(UsagePeriod.Month))
        advanceUntilIdle()
        assertEquals(ZonedDateTime.of(2026, 9, 19, 0, 0, 0, 0, rome).toInstant().toEpochMilli(), sessions.requests.last().first)
    }

    @Test
    fun `a failure keeps the last statistics and can be retried`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        sessions.result = Result.failure(OpenCodeError.Unreachable("offline"))
        vm.onEvent(UsageEvent.SelectPeriod(UsagePeriod.Month))
        advanceUntilIdle()
        assertNotNull(vm.state.value.error)
        assertNotNull(vm.state.value.stats)
        assertFalse(vm.state.value.loading)

        sessions.result = Result.success(stats())
        vm.onEvent(UsageEvent.Retry)
        advanceUntilIdle()
        assertNull(vm.state.value.error)
    }

    private fun midnight(day: Int) = ZonedDateTime.of(2026, 10, day, 0, 0, 0, 0, rome).toInstant().toEpochMilli()
}

private fun stats() = UsageStats(
    sessions = 3, subagents = 0, prompts = 10, steps = 40, cost = 42.0, tokens = TokenUsage(input = 100),
    activeDays = 2, streak = 1, activity = emptyList(), models = emptyList(),
)

private class FakeSessions : SessionRepository {
    var result: Result<UsageStats> = Result.success(stats())
    val requests = mutableListOf<Triple<Long, Long, String>>()
    override suspend fun usage(from: Long, to: Long, timeZone: String): Result<UsageStats> {
        requests += Triple(from, to, timeZone)
        return result
    }
    override suspend fun list(search: String?, directory: String?, limit: Int): Result<List<Session>> = Result.success(emptyList())
    override suspend fun active(): Result<Set<String>> = Result.success(emptySet())
    override suspend fun get(sessionId: String): Result<Session> = Result.failure(OpenCodeError.NotFound(sessionId))
    override suspend fun create(directory: String, agent: String?, model: ModelRef?): Result<Session> = Result.failure(OpenCodeError.NotFound(directory))
    override suspend fun rename(sessionId: String, title: String) = Result.success(Unit)
    override suspend fun delete(sessionId: String) = Result.success(Unit)
    override suspend fun fork(sessionId: String, beforeMessageId: String?): Result<Session> = Result.failure(OpenCodeError.NotFound(sessionId))
    override suspend fun projects(): Result<List<Project>> = Result.success(emptyList())
}

private class FakeCatalog : CatalogRepository {
    override suspend fun agents(directory: String?): Result<List<Agent>> = Result.success(emptyList())
    override suspend fun models(directory: String?) = Result.success(
        listOf(ModelInfo(ModelRef("haiku", "anthropic"), "Claude Haiku", "anthropic", null, 200_000, true, emptyList(), 0, true, "active", "Anthropic")),
    )
}
