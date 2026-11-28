package io.github.beppe2880.opencodemobile.core.data.lock

import io.github.beppe2880.opencodemobile.core.domain.AppLock
import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * [AppLock] driven by whether the app is in the foreground ([visible]): a launch with the lock on
 * starts locked, and coming back locks again once the app was away for
 * [AppSettings.appLockAfterMinutes]. With 0 it locks as soon as it goes to the background, so the
 * app never shows unlocked on return. The process lifecycle ignores rotations and other
 * configuration changes.
 */
internal class VisibilityAppLock(
    private val settings: SettingsRepository,
    visible: Flow<Boolean>,
    scope: CoroutineScope,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : AppLock {

    private val guard = SynchronizedObject()
    private val _locked = MutableStateFlow(settings.settings.value.appLock)
    override val locked: StateFlow<Boolean> = _locked.asStateFlow()
    private val _returns = MutableStateFlow(0)
    override val returns: StateFlow<Int> = _returns.asStateFlow()

    /** The app left the foreground on its own, not for the system authentication screen. */
    private var away = false

    /** When the app went to the background; null while it is in the foreground or unlocked since. */
    private var hiddenAt: Long? = null
    private var authenticating = false

    init {
        scope.launch {
            // Turning the lock off unlocks; turning it on doesn't lock what the user is looking at.
            settings.settings.map { it.appLock }.distinctUntilChanged().collect { on ->
                if (!on) synchronized(guard) { _locked.value = false }
            }
        }
        scope.launch {
            visible.collect { if (it) onShown() else onHidden() }
        }
    }

    override fun unlock() = synchronized(guard) {
        hiddenAt = null
        _locked.value = false
    }

    override fun authenticating(active: Boolean) = synchronized(guard) {
        authenticating = active
    }

    private fun onHidden() = synchronized(guard) {
        if (authenticating) return
        away = true
        val s = settings.settings.value
        if (!s.appLock) return
        hiddenAt = now()
        if (s.appLockAfterMinutes == 0) _locked.value = true
    }

    private fun onShown() = synchronized(guard) {
        if (!away) return
        away = false
        _returns.value++
        val s = settings.settings.value
        val since = hiddenAt ?: return
        hiddenAt = null
        if (s.appLock && now() - since >= s.appLockAfterMinutes * MINUTE) _locked.value = true
    }

    private companion object {
        const val MINUTE = 60_000L
    }
}
