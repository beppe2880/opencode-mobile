package io.github.beppe2880.opencodemobile.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme

/** The first letter of a provider [name] in a small rounded square: the mark of a provider without a logo. Decorative. */
@Composable
fun OcProviderInitial(name: String, modifier: Modifier = Modifier, size: Dp = 18.dp) {
    val c = OcTheme.colors
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(OcRadius.xs))
            .background(c.layer03),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.trim().firstOrNull()?.uppercase().orEmpty(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = (size.value * 0.6f).sp, lineHeight = (size.value * 0.6f).sp),
            fontWeight = FontWeight.SemiBold,
            color = c.textMuted,
            maxLines = 1,
        )
    }
}
