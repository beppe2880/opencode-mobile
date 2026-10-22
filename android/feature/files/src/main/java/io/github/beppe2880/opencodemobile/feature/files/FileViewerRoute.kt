package io.github.beppe2880.opencodemobile.feature.files

import android.content.ClipData
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.beppe2880.opencodemobile.core.mvi.CollectEffect
import io.github.beppe2880.opencodemobile.core.ui.text.asString
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import io.github.beppe2880.opencodemobile.core.ui.R as UiR

/** Read-only view of [path] (absolute, or relative to the session folder [directory]). */
@Composable
fun FileViewerRoute(directory: String, path: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel: FileViewerViewModel = koinViewModel(key = "file-$directory-$path") {
        parametersOf(FileTarget(directory, path))
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            FileViewerEffect.NavigateBack -> onBack()
            is FileViewerEffect.ShowMessage -> snackbar.showSnackbar(effect.message.asString(context))
        }
    }
    FileViewerScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onCopy = { text ->
            scope.launch {
                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(state.name, text)))
                snackbar.showSnackbar(context.getString(UiR.string.oc_copied))
            }
        },
        modifier = modifier,
        snackbarHostState = snackbar,
    )
}
