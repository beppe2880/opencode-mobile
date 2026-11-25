package io.github.beppe2880.opencodemobile.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SessionExportProgressTest {
    @Test
    fun percentagesMeasureWorkAndRejectUnreliableTotals() {
        fun progress(done: Long, total: Long?) = SessionExportProgress(SessionExportStage.Downloading, done, total).fraction
        assertEquals(0f, progress(0, 100))
        assertEquals(0.25f, progress(25, 100))
        assertEquals(1f, progress(100, 100))
        assertNull(progress(100, null))
        assertNull(progress(0, 0))
        assertNull(progress(5, -1))
        assertNull(progress(-1, 100))
        assertNull(progress(101, 100))
        assertEquals(0.5f, progress(Long.MAX_VALUE / 2, Long.MAX_VALUE))
    }
}
