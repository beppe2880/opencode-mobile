package io.github.beppe2880.opencodemobile.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme

/** Modal sheet with token colors. [skipPartiallyExpanded] for forms and long pickers. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
    skipPartiallyExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = OcTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded),
        modifier = (if (testTag != null) modifier.testTag(testTag) else modifier)
            .statusBarsPadding()
            .exposeTestTags(),
        shape = RoundedCornerShape(topStart = OcRadius.sheet, topEnd = OcRadius.sheet),
        containerColor = c.bgBase,
        contentColor = c.textBase,
        scrimColor = c.scrim,
        dragHandle = {
            // Visual-only pill: no focus tooltip or large touch highlight above the sheet.
            Box(
                Modifier
                    .padding(vertical = 22.dp)
                    .size(32.dp, 4.dp)
                    .background(c.layer04, RoundedCornerShape(2.dp)),
            )
        },
        content = content,
    )
}
