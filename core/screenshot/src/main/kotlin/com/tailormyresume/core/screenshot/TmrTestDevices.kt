package com.tailormyresume.core.screenshot

data class TmrTestDevice(
    val name: String,
    val qualifiers: String,
    val fontScale: Float,
)

object TmrTestDevices {
    const val BOARD_QUALIFIERS = "w393dp-h852dp-normal-long-notround-any-440dpi-keyshidden-nonav"
    const val SMALL_PHONE_QUALIFIERS = "w320dp-h568dp-normal-long-notround-any-xhdpi-keyshidden-nonav"
    const val DEFAULT_FONT_SCALE = 1.0f
    const val LARGE_FONT_SCALE = 2.0f

    const val PROTOTYPE_QUALIFIERS = "w374dp-h834dp-normal-long-notround-any-480dpi-keyshidden-nonav"
    val prototype = TmrTestDevice("prototype", PROTOTYPE_QUALIFIERS, DEFAULT_FONT_SCALE)
    val prototypeLargeFont = TmrTestDevice("prototype-font-200", PROTOTYPE_QUALIFIERS, LARGE_FONT_SCALE)
    val board = TmrTestDevice("board", BOARD_QUALIFIERS, DEFAULT_FONT_SCALE)
    val boardLargeFont = TmrTestDevice("board-font-200", BOARD_QUALIFIERS, LARGE_FONT_SCALE)
    val smallPhone = TmrTestDevice("small-phone", SMALL_PHONE_QUALIFIERS, DEFAULT_FONT_SCALE)

    val all: List<TmrTestDevice> = listOf(prototype, prototypeLargeFont, board, boardLargeFont, smallPhone)
}
