package io.github.beppe2880.opencodemobile.core.data.events

import kotlin.test.Test
import kotlin.test.assertEquals

class PathReporterTest {

    private val wifi = setOf("en0")
    private val cellular = setOf("pdp_ip0")

    @Test
    fun `the first path is always reported, usable or not`() {
        assertEquals(true, PathReporter().onPath(true, wifi))
        assertEquals(false, PathReporter().onPath(false, emptySet()))
    }

    @Test
    fun `only changes are reported afterwards`() {
        val reporter = PathReporter()
        val reports = listOf(
            reporter.onPath(true, wifi),
            reporter.onPath(true, wifi),
            reporter.onPath(false, emptySet()),
            reporter.onPath(false, emptySet()),
            reporter.onPath(true, wifi),
        )

        assertEquals(listOf(true, null, false, null, true), reports)
    }

    @Test
    fun `a usable path on other interfaces is a new network`() {
        val reporter = PathReporter()
        reporter.onPath(true, wifi)

        assertEquals(true, reporter.onPath(true, cellular))
        assertEquals(null, reporter.onPath(true, cellular))
    }

    @Test
    fun `interface changes while unusable are not reported`() {
        val reporter = PathReporter()
        reporter.onPath(false, wifi)

        assertEquals(null, reporter.onPath(false, cellular))
    }
}
