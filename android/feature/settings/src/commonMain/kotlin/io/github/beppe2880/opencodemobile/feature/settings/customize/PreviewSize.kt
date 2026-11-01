package io.github.beppe2880.opencodemobile.feature.settings.customize

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_preview_half
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_preview_hide
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_preview_large
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_preview_small
import org.jetbrains.compose.resources.StringResource
import kotlin.math.max

/** Quick sizes of the preview above its options on narrow screens, as a share of the height. */
internal enum class PreviewSize(val fraction: Float, val label: StringResource) {
    Hidden(0f, Res.string.settings_preview_hide),
    Small(0.25f, Res.string.settings_preview_small),
    Half(0.5f, Res.string.settings_preview_half),
    Large(0.7f, Res.string.settings_preview_large),
    ;

    companion object {
        /** Until the user moves the divider: enough to see a few parts, the options in thumb reach. */
        const val DEFAULT_FRACTION = 0.4f
    }
}

/** The preview never gets smaller than this (a title bar and a row at large text); it hides instead. */
internal val MinPreviewHeight = 140.dp

/** The options always keep at least this much: a part name and a couple of controls. */
internal val MinOptionsHeight = 200.dp

/** Height of the preview for [fraction] of [available]: 0 hides it, else within the minimums. */
internal fun previewHeight(fraction: Float, available: Dp): Dp {
    if (fraction <= 0f) return 0.dp
    val max = max(MinPreviewHeight.value, (available - MinOptionsHeight).value).dp
    return (available * fraction).coerceIn(MinPreviewHeight, max)
}

/** The share to keep for a preview dragged to [height]: under half its minimum it hides. */
internal fun snapFraction(height: Dp, available: Dp): Float = when {
    available <= 0.dp || height < MinPreviewHeight / 2 -> 0f
    else -> (previewHeight(height / available, available) / available).coerceIn(0f, 1f)
}
