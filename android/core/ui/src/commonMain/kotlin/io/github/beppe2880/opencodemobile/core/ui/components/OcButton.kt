package io.github.beppe2880.opencodemobile.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme

enum class OcButtonVariant { Primary, Secondary, Ghost, Danger, Inverse }

/**
 * Standard button. Primary = accent fill, Secondary = hairline outlined surface,
 * Ghost = text only, Danger = destructive fill, Inverse = high-contrast neutral fill.
 */
@Composable
fun OcButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: OcButtonVariant = OcButtonVariant.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    compact: Boolean = false,
) {
    val colors = OcTheme.colors
    val (container, content) = when (variant) {
        OcButtonVariant.Primary -> colors.bgAccent to colors.onAccent
        OcButtonVariant.Secondary -> colors.bgBase to colors.textBase
        OcButtonVariant.Ghost -> Color.Transparent to colors.textBase
        OcButtonVariant.Danger -> colors.danger.fg to colors.bgBase
        OcButtonVariant.Inverse -> colors.bgInverse to colors.textInverse
    }
    val border = if (variant == OcButtonVariant.Secondary) BorderStroke(1.dp, colors.borderStrong) else null
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.heightIn(min = if (compact) 36.dp else 44.dp),
        shape = RoundedCornerShape(OcRadius.md),
        border = border,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = if (variant == OcButtonVariant.Ghost) Color.Transparent else colors.layer02,
            disabledContentColor = colors.textFaint,
        ),
        contentPadding = PaddingValues(
            horizontal = if (compact) OcSpacing.md else OcSpacing.lg,
            vertical = if (compact) OcSpacing.xs else OcSpacing.sm,
        ),
        elevation = null,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
        ) {
            // The spinner crossfades with the icon it replaces (or in, without one).
            if (loading || leadingIcon != null) {
                OcCrossfade(loading, label = "button_loading") { busy ->
                    when {
                        busy -> CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = colors.textMuted,
                        )
                        leadingIcon != null -> Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Text(text = text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** 48dp touch target icon button with token tint. */
@Composable
fun OcIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = OcTheme.colors.iconBase,
    iconSize: Dp = 20.dp,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = tint,
            disabledContentColor = OcTheme.colors.textFaint,
        ),
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(iconSize))
    }
}

/** Round filled action, used for the composer send/stop buttons. */
@Composable
fun OcRoundIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    container: Color = OcTheme.colors.bgInverse,
    content: Color = OcTheme.colors.textInverse,
    size: Dp = 36.dp,
) {
    val colors = OcTheme.colors
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Color.Transparent,
                contentColor = content,
                disabledContentColor = colors.textFaint,
            ),
        ) {
            androidx.compose.material3.Surface(
                shape = CircleShape,
                color = if (enabled) container else colors.layer03,
                contentColor = if (enabled) content else colors.textFaint,
                modifier = Modifier.size(size),
            ) {
                // A new icon (Send while running, arrow otherwise) crossfades with the old one.
                OcCrossfade(icon, label = "round_icon") { shown ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(shown, contentDescription = contentDescription, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
