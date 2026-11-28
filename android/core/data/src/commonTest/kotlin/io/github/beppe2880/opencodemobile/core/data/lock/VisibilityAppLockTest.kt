package io.github.beppe2880.opencodemobile.core.data.lock

import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VisibilityAppLockTest {

    private class FakeSettings(initial: AppSettings) : SettingsRepository {
        override val settings = MutableStateFlow(initial)
        override suspend fun update(transform: (AppSettings) -> AppSettings) {
            settings.value = transform(settings.value)
        }
    }

    private val visible = MutableStateFlow(true)
    private var clock = 0L

    private fun TestScope.lock(settings: AppSettings): Pair<VisibilityAppLock, FakeSettings> {
        val repository = FakeSettings(settings)
        return VisibilityAppLock(repository, visible, backgroundScope, now = { clock }).also { runCurrent() } to repository
    }

    private fun TestScope.away(minutes: Int) {
        visible.value = false
        runCurrent()
        clock += minutes * 60_000L
        visible.value = true
        runCurrent()
    }

    @Test
    fun `a launch starts locked only with the lock on`() = runTest {
        assertTrue(lock(AppSettings(appLock = true)).first.locked.value)
        assertFalse(lock(AppSettings(appLock = false)).first.locked.value)
    }

    @Test
    fun `every time mode locks as soon as the app goes to the background`() = runTest {
        val (lock, _) = lock(AppSettings(appLock = true))
        lock.unlock()

        visible.value = false
        runCurrent()

        assertTrue(lock.locked.value)
    }

    @Test
    fun `a short absence keeps the app unlocked and a long one locks it`() = runTest {
        val (lock, _) = lock(AppSettings(appLock = true, appLockAfterMinutes = 5))
        lock.unlock()

        away(minutes = 4)
        assertFalse(lock.locked.value)

        away(minutes = 5)
        assertTrue(lock.locked.value)
    }

    @Test
    fun `time spent in the system authentication screen does not count`() = runTest {
        val (lock, _) = lock(AppSettings(appLock = true))
        lock.authenticating(true)

        val returns = lock.returns.value
        away(minutes = 10)
        assertTrue(lock.locked.value)
        lock.authenticating(false)
        // The system screen closed after the app came back: still not a return of the user.
        assertEquals(returns, lock.returns.value)
        lock.unlock()

        assertFalse(lock.locked.value)
        away(minutes = 0)
        assertEquals(returns + 1, lock.returns.value)
    }

    @Test
    fun `turning the lock off unlocks and turning it on does not lock the open app`() = runTest {
        val (lock, settings) = lock(AppSettings(appLock = true))
        settings.update { it.copy(appLock = false) }
        runCurrent()
        assertFalse(lock.locked.value)

        settings.update { it.copy(appLock = true) }
        runCurrent()
        assertFalse(lock.locked.value)

        away(minutes = 0)
        assertTrue(lock.locked.value)
    }
}
