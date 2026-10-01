package com.taskchain.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class CountdownProgressTest {
    @Test
    fun paintsElapsedShareAndStaysFullInOvertime() {
        assertEquals(0f, countdownProgress(60, 60_000), 0f)
        assertEquals(0.5f, countdownProgress(60, 30_000), 0f)
        assertEquals(1f, countdownProgress(60, 0), 0f)
        assertEquals(1f, countdownProgress(60, -5_000), 0f)
        assertEquals(0f, countdownProgress(null, null), 0f)
    }
}
