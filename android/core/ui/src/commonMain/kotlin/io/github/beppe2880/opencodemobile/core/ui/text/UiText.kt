package io.github.beppe2880.opencodemobile.core.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import io.github.beppe2880.opencodemobile.core.domain.OpenCodeError
import io.github.beppe2880.opencodemobile.core.ui.resources.Res
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_error_generic
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_error_not_found
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_error_pairing
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_error_server
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_error_unauthorized
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_error_unreachable
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Localizable text that ViewModels can put in State without a Context. */
@Immutable
sealed interface UiText {
    data class Resource(val id: StringResource, val args: List<Any> = emptyList()) : UiText
    data class Raw(val value: String) : UiText

    /** A count phrase: [quantity] picks the plural form, [args] fill its placeholders. */
    data class Plural(val id: PluralStringResource, val quantity: Int, val args: List<Any> = emptyList()) : UiText

    @Composable
    fun resolve(): String = when (this) {
        is Resource -> stringResource(id, *args.toTypedArray())
        is Raw -> value
        is Plural -> pluralStringResource(id, quantity, *args.toTypedArray())
    }
}

/** Maps repository failures to user-facing messages, per [OpenCodeError] type. */
fun Throwable.toUiText(): UiText = when (this) {
    is OpenCodeError.Unauthorized -> UiText.Resource(Res.string.oc_error_unauthorized)
    is OpenCodeError.Unreachable -> UiText.Resource(Res.string.oc_error_unreachable)
    is OpenCodeError.PairingFailed -> UiText.Resource(Res.string.oc_error_pairing)
    is OpenCodeError.NotFound -> UiText.Resource(Res.string.oc_error_not_found)
    is OpenCodeError.Server -> UiText.Resource(Res.string.oc_error_server, listOf(status, message ?: tag ?: ""))
    else -> UiText.Resource(Res.string.oc_error_generic, listOf(message ?: this::class.simpleName ?: ""))
}

/** Resolves outside composition, e.g. for snackbars shown from effects. */
suspend fun UiText.asString(): String = when (this) {
    is UiText.Raw -> value
    is UiText.Resource -> getString(id, *args.toTypedArray())
    is UiText.Plural -> getPluralString(id, quantity, *args.toTypedArray())
}
