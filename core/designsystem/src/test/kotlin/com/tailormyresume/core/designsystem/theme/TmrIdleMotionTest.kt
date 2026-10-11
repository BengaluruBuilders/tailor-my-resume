package com.tailormyresume.core.designsystem.theme

import androidx.compose.ui.unit.dp
import org.junit.Test
import kotlin.math.sin
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TmrIdleMotionTest {
    @Test
    fun defaultIdleMatchesPlanAndReducedIsStatic() {
        val idle = TmrMotionDefaults.Default.idle
        assertEquals(5.dp, idle.bobAmplitude)
        assertEquals(420f, idle.bobAngularPeriodMs)
        assertEquals(4, idle.bobPhaseMax)
        assertEquals(4f, idle.wobbleDegrees)
        assertEquals(300f, idle.wobbleAngularPeriodMs)
        assertEquals(6.dp, idle.wobbleOffset)
        assertEquals(210f, idle.wobbleOffsetAngularPeriodMs)
        assertEquals(5000, idle.storyDurationMs)
        assertEquals(900, idle.spinnerTurnMs)
        assertTrue(idle.storyAutoAdvance)
        assertTrue(idle.spinnerAnimated)

        val reduced = TmrMotionDefaults.Reduced.idle
        assertEquals(0.dp, reduced.bobAmplitude)
        assertEquals(0f, reduced.wobbleDegrees)
        assertEquals(0.dp, reduced.wobbleOffset)
        assertFalse(reduced.storyAutoAdvance)
        assertFalse(reduced.spinnerAnimated)
    }

    @Test
    fun bobOffsetFollowsFiveTimesSineOfTimeOverPeriodPlusPhase() {
        val idle = TmrMotionDefaults.Default.idle
        val expected = (5 * sin(1000f / 420f + 2f)).toDouble()
        assertEquals(expected, idle.bobOffsetDp(1000L, 2).toDouble(), 0.001)
        assertEquals(0.0, TmrMotionDefaults.Reduced.idle.bobOffsetDp(1000L, 2).toDouble(), 0.0001)
    }
}
