package io.github.beppe2880.opencodemobile.core.data.lock

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Locking the iPhone sends the app to the background, which the app visibility already reports. */
internal class AlwaysOnScreenState : ScreenState {
    override val on: Flow<Boolean> = flowOf(true)
}
