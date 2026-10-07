package com.taskchain.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/** Covers the builder's persisted-seconds to visible minutes/seconds conversion. */
class BuilderPunchDurationFormatTest {
    /** Use this function to verify persisted seconds render as the expected minute and second pair.
     * Inputs: none.
     * Dependencies: `routineDurationParts`.
     */
    @Test
    fun routineDurationParts_preservesMinuteAndSecondValues() {
        assertEquals(2L to 5L, routineDurationParts(125L))
        assertEquals(0L to 0L, routineDurationParts(0L))
    }
}
