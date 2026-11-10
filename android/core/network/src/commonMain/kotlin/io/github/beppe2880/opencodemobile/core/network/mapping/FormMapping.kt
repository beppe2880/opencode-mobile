package io.github.beppe2880.opencodemobile.core.network.mapping

import io.github.beppe2880.opencodemobile.api.models.FormInfo
import io.github.beppe2880.opencodemobile.api.models.FormWhen
import io.github.beppe2880.opencodemobile.core.domain.Form
import io.github.beppe2880.opencodemobile.core.domain.FormAnswer
import io.github.beppe2880.opencodemobile.core.domain.FormCondition
import io.github.beppe2880.opencodemobile.core.domain.FormField
import io.github.beppe2880.opencodemobile.core.domain.FormOption
import io.github.beppe2880.opencodemobile.core.domain.defaultAnswer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import io.github.beppe2880.opencodemobile.api.models.FormField as ApiField
import io.github.beppe2880.opencodemobile.api.models.FormOption as ApiOption

fun FormInfo.toDomain(): Form {
    val visible = mutableListOf<FormField>()
    val preset = mutableMapOf<String, FormAnswer>()
    for (field in fields) {
        val mapped = field.toDomain()
        if (field.isHidden()) mapped.defaultAnswer()?.let { preset[mapped.key] = it } else visible += mapped
    }
    return Form(id = id, sessionId = sessionID, title = title, fields = visible, presetAnswers = preset)
}

fun ApiField.isHidden(): Boolean = when (this) {
    is ApiField.StringWrapper -> value.hidden
    is ApiField.BooleanWrapper -> value.hidden
    is ApiField.MultiselectWrapper -> value.hidden
    is ApiField.NumberWrapper -> value.hidden
    is ApiField.IntegerWrapper -> value.hidden
    is ApiField.ExternalWrapper -> false
} == true

fun ApiField.toDomain(): FormField = when (this) {
    is ApiField.StringWrapper -> with(value) {
        FormField.Text(
            key = key, title = title, description = description, required = required == true,
            conditions = `when`.toConditions(), options = options.toOptions(), custom = custom == true,
            placeholder = placeholder, default = default,
            minLength = minLength?.toInt(), maxLength = maxLength?.toInt(), pattern = pattern,
        )
    }
    is ApiField.BooleanWrapper -> with(value) {
        FormField.Toggle(key, title, description, required == true, `when`.toConditions(), default)
    }
    is ApiField.MultiselectWrapper -> with(value) {
        FormField.MultiSelect(
            key = key, title = title, description = description, required = required == true,
            conditions = `when`.toConditions(), options = options.toOptions(), custom = custom == true,
            minItems = minItems?.toInt(), maxItems = maxItems?.toInt(), default = default.orEmpty(),
        )
    }
    is ApiField.NumberWrapper -> with(value) {
        FormField.Number(
            key, title, description, required == true, `when`.toConditions(),
            integer = false, min = minimum.number(), max = maximum.number(), default = default.number(),
        )
    }
    is ApiField.IntegerWrapper -> with(value) {
        FormField.Number(
            key, title, description, required == true, `when`.toConditions(),
            integer = true, min = minimum.number(), max = maximum.number(), default = default.number(),
        )
    }
    is ApiField.ExternalWrapper -> with(value) { FormField.External(key, url, title, description) }
}

fun FormAnswer.toJson(): JsonElement = when (this) {
    is FormAnswer.Text -> JsonPrimitive(value)
    is FormAnswer.Bool -> JsonPrimitive(value)
    is FormAnswer.Number -> if (value % 1.0 == 0.0) JsonPrimitive(value.toLong()) else JsonPrimitive(value)
    is FormAnswer.Choices -> JsonArray(values.map(::JsonPrimitive))
}

private fun JsonElement?.toAnswer(): FormAnswer? {
    val primitive = (this as? JsonPrimitive) ?: return (this as? JsonArray)?.let { array ->
        FormAnswer.Choices(array.map { it.jsonPrimitive.content })
    }
    return when {
        primitive.isString -> FormAnswer.Text(primitive.content)
        primitive.booleanOrNull != null -> FormAnswer.Bool(primitive.booleanOrNull!!)
        primitive.doubleOrNull != null -> FormAnswer.Number(primitive.doubleOrNull!!)
        else -> null
    }
}

private fun List<FormWhen>?.toConditions(): List<FormCondition> =
    orEmpty().map { FormCondition(it.key, it.op == FormWhen.Op.EQ, it.`value`.toAnswer()) }

private fun List<ApiOption>?.toOptions(): List<FormOption> =
    orEmpty().map { FormOption(it.`value`, it.label, it.description) }

private fun JsonElement?.number(): Double? = (this as? JsonPrimitive)?.doubleOrNull
