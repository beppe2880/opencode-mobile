package io.github.beppe2880.opencodemobile.core.data.lock

import kotlinx.coroutines.flow.Flow

/**
 * Whether the device screen is on. Some Android phones only pause the app when the screen turns
 * off, so the process still counts as visible: the app lock needs the screen too.
 */
internal interface ScreenState {
    val on: Flow<Boolean>
}
