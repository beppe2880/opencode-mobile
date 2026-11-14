package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/** Where the thumb of an [io.github.beppe2880.opencodemobile.core.ui.components.OcSliderRow] is at [fraction] (0..1) of its track. */
internal fun sliderThumb(row: Rect, fraction: Float, density: Density): Offset = with(density) {
    val start = row.left + SLIDER_INSET.toPx()
    val end = row.right - SLIDER_INSET.toPx()
    Offset(start + (end - start) * fraction, row.bottom - 32.dp.toPx())
}

/** The fraction (0..1) of the track of a slider row that [x] is at. */
internal fun sliderFraction(row: Rect, x: Float, density: Density): Float = with(density) {
    val start = row.left + SLIDER_INSET.toPx()
    val end = row.right - SLIDER_INSET.toPx()
    ((x - start) / (end - start)).coerceIn(0f, 1f)
}

private val SLIDER_INSET = 26.dp

internal val DEMO_PICTURE = Brush.linearGradient(listOf(Color(0xFFF08A5D), Color(0xFFB83B5E), Color(0xFF6A2C70)))
internal val DEMO_LANDSCAPE = Brush.verticalGradient(listOf(Color(0xFF7FB3E8), Color(0xFFF6D7A7), Color(0xFF6E9B5C)))
