package com.kmab.deft.feature.reader

internal enum class ReaderTapAction { LEFT, CHROME, RIGHT }

internal fun readerTapAction(
    x: Float,
    width: Int,
    tapToTurn: Boolean,
): ReaderTapAction {
    val edge = width * TAP_EDGE_FRACTION
    return when {
        !tapToTurn || x in edge..(width - edge) -> ReaderTapAction.CHROME
        x < width / 2f -> ReaderTapAction.LEFT
        else -> ReaderTapAction.RIGHT
    }
}

private const val TAP_EDGE_FRACTION = 0.4f
