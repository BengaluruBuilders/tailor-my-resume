package com.tailormyresume.core.designsystem.theme

import com.tailormyresume.core.screenshot.TmrTestDevices
import org.junit.Test
import kotlin.test.assertEquals

class TmrScreenshotDevicesTest {
    @Test
    fun prototypeDeviceIsDefault() {
        assertEquals(
            "w374dp-h834dp-normal-long-notround-any-480dpi-keyshidden-nonav",
            TmrTestDevices.PROTOTYPE_QUALIFIERS,
        )
        assertEquals(1.0f, TmrTestDevices.prototype.fontScale)
        assertEquals(2.0f, TmrTestDevices.prototypeLargeFont.fontScale)
        assertEquals(TmrTestDevices.PROTOTYPE_QUALIFIERS, TmrTestDevices.prototype.qualifiers)
        assertEquals(
            listOf(
                TmrTestDevices.prototype,
                TmrTestDevices.prototypeLargeFont,
                TmrTestDevices.board,
                TmrTestDevices.boardLargeFont,
                TmrTestDevices.smallPhone,
            ),
            TmrTestDevices.all,
        )
        assertEquals("w393dp-h852dp-normal-long-notround-any-440dpi-keyshidden-nonav", TmrTestDevices.BOARD_QUALIFIERS)
    }
}
