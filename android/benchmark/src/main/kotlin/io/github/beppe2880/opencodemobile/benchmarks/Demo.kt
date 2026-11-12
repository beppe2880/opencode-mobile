package io.github.beppe2880.opencodemobile.benchmarks

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import java.util.regex.Pattern

/** The app's `benchmark` build type: release code started in the offline demo. */
const val TARGET_PACKAGE = "io.github.beppe2880.opencodemobile.benchmark"

private const val WAIT_MS = 10_000L

/** The session list of the demo; test tags are exposed as resource ids. */
val SessionList: BySelector = By.res("sessions_list")

/** The running mark of a session row; the demo always has sessions at work. */
val RunningMark: BySelector = By.res(Pattern.compile("session_running_.*"))

val ChatComposer: BySelector = By.res("chat_composer_field")

fun MacrobenchmarkScope.waitFor(selector: BySelector): UiObject2 =
    device.wait(Until.findObject(selector), WAIT_MS) ?: throw AssertionError("Not on screen after ${WAIT_MS}ms: $selector")

/** Starts the app and waits for the demo's session list. */
fun MacrobenchmarkScope.startDemo() {
    startActivityAndWait()
    waitFor(SessionList)
}

/** A fresh process on the demo's session list, whatever screen the last run left open. */
fun MacrobenchmarkScope.restartDemo() {
    pressHome()
    killProcess()
    startDemo()
}

/** Opens the chat of a session that is running in the demo. */
fun MacrobenchmarkScope.openRunningChat() {
    waitFor(RunningMark).click()
    waitFor(ChatComposer)
}

/** Opens the chat of a session that is not running, for a screen with nothing animated. */
fun MacrobenchmarkScope.openIdleChat() {
    val running = device.findObjects(RunningMark).map { it.resourceName.removePrefix("session_running_") }.toSet()
    val row = device.findObjects(By.res(Pattern.compile("session_row_.*")))
        .firstOrNull { it.resourceName.removePrefix("session_row_") !in running }
        ?: throw AssertionError("The demo shows no idle session")
    row.click()
    waitFor(ChatComposer)
}
