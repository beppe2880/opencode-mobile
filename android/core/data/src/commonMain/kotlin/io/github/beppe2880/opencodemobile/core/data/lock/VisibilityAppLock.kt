package io.github.beppe2880.opencodemobile.core.data.lock

import io.github.beppe2880.opencodemobile.core.domain.AppLock
import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * [AppLock] driven by whether the app is in the foreground ([appVisible], from the process
 * lifecycle, which ignores rotations) and whether the screen is on ([screenOn]).
 *
 * Two absences count separately: leaving for other apps or the home screen
 * ([AppSettings.appLockOnLeave]) and turning the screen off while the app is open
 * ([AppSettings.appLockOnScreenOff]), each with its own minutes. With 0 the app locks as soon as
 * it is left, so it never shows unlocked on return. A launch with the lock on starts locked. Before
 * asking, [trusted] can open the app on a trusted network.
 */
internal class VisibilityAppLock(
    private val settings: SettingsRepository,
    appVisible: Flow<Boolean>,
    screenOn: Flow<Boolean>,
    private val scope: CoroutineScope,
    private val trusted: suspend () -> Boolean = { false },
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : AppLock {

    private val guard = SynchronizedObject()
    private val _locked = MutableStateFlow(settings.settings.value.appLock)
    override val locked: StateFlow<Boolean> = _locked.asStateFlow()
    private val _promptRequests = MutableStateFlow(0)
    override val promptRequests: StateFlow<Int> = _promptRequests.asStateFlow()

    private var visible = true
    private var screen = true

    /** When the user left for other apps, or turned the screen off in the app; null otherwise. */
    private var leftAt: Long? = null
    private var screenOffAt: Long? = null
    private var authenticating = false
    private var check: Job? = null

    init {
        scope.launch {
            // Turning the lock off unlocks; turning it on doesn't lock what the user is looking at.
            settings.settings.map { it.appLock }.distinctUntilChanged().collect { on ->
                if (!on) synchronized(guard) { _locked.value = false }
            }
        }
        scope.launch { appVisible.collect(::onAppVisible) }
        scope.launch { screenOn.collect(::onScreen) }
        if (_locked.value) checkThenAsk()
    }

    override fun unlock() = synchronized(guard) {
        check?.cancel()
        leftAt = null
        screenOffAt = null
        _locked.value = false
    }

    override fun authenticating(active: Boolean) = synchronized(guard) {
        authenticating = active
    }

    private fun onAppVisible(value: Boolean) = synchronized(guard) {
        val was = visible && screen
        visible = value
        // The system authentication screen covers the app: not the user leaving.
        if (!value && was && !authenticating && leftAt == null) {
            leftAt = now()
            val s = settings.settings.value
            if (s.appLock && s.appLockOnLeave && s.appLockAfterMinutes == 0) _locked.value = true
        }
        if (value && screen) onBack()
    }

    private fun onScreen(on: Boolean) = synchronized(guard) {
        val wasShown = visible && screen
        screen = on
        // Only the screen turned off while the app was open: leaving it first is a leave.
        if (!on && wasShown && !authenticating && screenOffAt == null) {
            screenOffAt = now()
            val s = settings.settings.value
            if (s.appLock && s.appLockOnScreenOff && s.appLockScreenOffMinutes == 0) _locked.value = true
        }
        if (on && visible) onBack()
    }

    /** The app is in front again with the screen on: lock when an absence was long enough. */
    private fun onBack() {
        val left = leftAt
        val off = screenOffAt
        if (left == null && off == null) return
        leftAt = null
        screenOffAt = null
        val s = settings.settings.value
        if (!s.appLock) return
        val time = now()
        val lock = _locked.value ||
            (left != null && s.appLockOnLeave && time - left >= s.appLockAfterMinutes * MINUTE) ||
            (off != null && s.appLockOnScreenOff && time - off >= s.appLockScreenOffMinutes * MINUTE)
        if (!lock) return
        _locked.value = true
        checkThenAsk()
    }

    /** Opens the app on a trusted network, or asks the lock screen to show the system prompt. */
    private fun checkThenAsk() {
        check?.cancel()
        check = scope.launch {
            val trusted = try {
                trusted()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                false
            }
            synchronized(guard) {
                if (!_locked.value) return@launch
                if (trusted) {
                    _locked.value = false
                } else {
                    _promptRequests.value++
                }
            }
        }
    }

    private companion object {
        const val MINUTE = 60_000L
    }
}
