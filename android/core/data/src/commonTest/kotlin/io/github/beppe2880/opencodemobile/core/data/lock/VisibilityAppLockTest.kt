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
    private val screen = MutableStateFlow(true)
    private var clock = 0L
    private var trusted = false

    private fun TestScope.lock(settings: AppSettings): Pair<VisibilityAppLock, FakeSettings> {
        val repository = FakeSettings(settings)
        val lock = VisibilityAppLock(repository, visible, screen, backgroundScope, trusted = { trusted }, now = { clock })
        runCurrent()
        return lock to repository
    }

    /** Other apps or the home screen for [minutes]. */
    private fun TestScope.leave(minutes: Int) {
        visible.value = false
        runCurrent()
        clock += minutes * 60_000L
        visible.value = true
        runCurrent()
    }

    /** The screen off for [minutes] with the app open, as on phones that only pause it. */
    private fun TestScope.screenOff(minutes: Int) {
        screen.value = false
        runCurrent()
        clock += minutes * 60_000L
        screen.value = true
        runCurrent()
    }

    @Test
    fun `a launch starts locked only with the lock on and then asks`() = runTest {
        val (lock, _) = lock(AppSettings(appLock = true))
        assertTrue(lock.locked.value)
        assertEquals(1, lock.promptRequests.value)
        assertFalse(lock(AppSettings(appLock = false)).first.locked.value)
    }

    @Test
    fun `every time locks as soon as the app is left`() = runTest {
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

        leave(minutes = 4)
        assertFalse(lock.locked.value)

        leave(minutes = 5)
        assertTrue(lock.locked.value)
    }

    @Test
    fun `leaving and turning the screen off follow their own switches and minutes`() = runTest {
        val (lock, settings) = lock(AppSettings(appLock = true, appLockOnLeave = false, appLockScreenOffMinutes = 2))
        lock.unlock()

        leave(minutes = 30)
        assertFalse(lock.locked.value, "Leaving does not lock when its switch is off")

        screenOff(minutes = 1)
        assertFalse(lock.locked.value)
        screenOff(minutes = 2)
        assertTrue(lock.locked.value)

        lock.unlock()
        settings.update { it.copy(appLockOnLeave = true, appLockOnScreenOff = false) }
        screenOff(minutes = 30)
        assertFalse(lock.locked.value, "Turning the screen off does not lock when its switch is off")
        leave(minutes = 0)
        assertTrue(lock.locked.value)
    }

    @Test
    fun `the screen turned off after leaving the app counts as leaving`() = runTest {
        val (lock, _) = lock(AppSettings(appLock = true, appLockAfterMinutes = 15, appLockScreenOffMinutes = 0))
        lock.unlock()

        visible.value = false
        runCurrent()
        screen.value = false
        runCurrent()
        clock += 60_000L
        screen.value = true
        visible.value = true
        runCurrent()

        assertFalse(lock.locked.value)
    }

    @Test
    fun `a trusted network opens the app without asking`() = runTest {
        trusted = true
        val (lock, _) = lock(AppSettings(appLock = true))
        assertFalse(lock.locked.value)
        assertEquals(0, lock.promptRequests.value)

        leave(minutes = 0)
        assertFalse(lock.locked.value)

        trusted = false
        leave(minutes = 0)
        assertTrue(lock.locked.value)
        assertEquals(1, lock.promptRequests.value)
    }

    @Test
    fun `time spent in the system authentication screen neither counts nor asks again`() = runTest {
        val (lock, _) = lock(AppSettings(appLock = true))
        val requests = lock.promptRequests.value
        lock.authenticating(true)

        leave(minutes = 10)
        screenOff(minutes = 10)
        lock.authenticating(false)
        assertEquals(requests, lock.promptRequests.value)
        lock.unlock()

        assertFalse(lock.locked.value)
        leave(minutes = 0)
        assertEquals(requests + 1, lock.promptRequests.value)
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

        leave(minutes = 0)
        assertTrue(lock.locked.value)
    }

    @Test
    fun `subnets contain the addresses of their network only`() {
        val home = Subnet(ipv4Bytes("192.168.100.38")!!, 24)
        assertTrue(ipv4Bytes("192.168.100.7")!! in home)
        assertFalse(ipv4Bytes("192.168.1.7")!! in home)
        assertTrue(ipv4Bytes("10.1.2.3")!! in Subnet(ipv4Bytes("10.0.0.1")!!, 8))
        assertEquals("192.168.100.38", hostOf("http://192.168.100.38:4096/"))
        assertEquals(null, ipv4Bytes(hostOf("https://server.local")))
    }
}
