package io.github.beppe2880.opencodemobile.core.domain

/** A question the agent asks the user (OpenCode "form"), answered field by field. */
data class Form(
    val id: String,
    val sessionId: String,
    val title: String,
    val fields: List<FormField>,
    /** Defaults of hidden fields, sent with every reply. */
    val presetAnswers: Map<String, FormAnswer> = emptyMap(),
)

/** One input of a [Form]. Hidden fields are dropped in mapping; [conditions] gate visibility. */
sealed interface FormField {
    val key: String
    val title: String?
    val description: String?
    val required: Boolean
    val conditions: List<FormCondition>

    /** Free text, or a single choice among [options] (plus free text when [custom]). */
    data class Text(
        override val key: String,
        override val title: String? = null,
        override val description: String? = null,
        override val required: Boolean = false,
        override val conditions: List<FormCondition> = emptyList(),
        val options: List<FormOption> = emptyList(),
        val custom: Boolean = false,
        val placeholder: String? = null,
        val default: String? = null,
        val minLength: Int? = null,
        val maxLength: Int? = null,
        val pattern: String? = null,
    ) : FormField

    data class Toggle(
        override val key: String,
        override val title: String? = null,
        override val description: String? = null,
        override val required: Boolean = false,
        override val conditions: List<FormCondition> = emptyList(),
        val default: Boolean? = null,
    ) : FormField

    data class MultiSelect(
        override val key: String,
        override val title: String? = null,
        override val description: String? = null,
        override val required: Boolean = false,
        override val conditions: List<FormCondition> = emptyList(),
        val options: List<FormOption>,
        val custom: Boolean = false,
        val minItems: Int? = null,
        val maxItems: Int? = null,
        val default: List<String> = emptyList(),
    ) : FormField

    data class Number(
        override val key: String,
        override val title: String? = null,
        override val description: String? = null,
        override val required: Boolean = false,
        override val conditions: List<FormCondition> = emptyList(),
        val integer: Boolean = false,
        val min: Double? = null,
        val max: Double? = null,
        val default: Double? = null,
    ) : FormField

    /** A link the user must open elsewhere (e.g. an OAuth page); it carries no answer. */
    data class External(
        override val key: String,
        val url: String,
        override val title: String? = null,
        override val description: String? = null,
    ) : FormField {
        override val required: Boolean get() = false
        override val conditions: List<FormCondition> get() = emptyList()
    }
}

data class FormOption(val value: String, val label: String, val description: String? = null)

/** Shows a field only when the answer of [key] equals ([equals] = true) or differs from [value]. */
data class FormCondition(val key: String, val equals: Boolean, val value: FormAnswer?)

sealed interface FormAnswer {
    data class Text(val value: String) : FormAnswer
    data class Bool(val value: Boolean) : FormAnswer
    data class Number(val value: Double) : FormAnswer
    data class Choices(val values: List<String>) : FormAnswer
}

/** Whether [field] is visible given the current [answers]. */
fun FormField.isVisible(answers: Map<String, FormAnswer>): Boolean = conditions.all { condition ->
    (answers[condition.key] == condition.value) == condition.equals
}

/** The answer a field starts with, if it has a default. */
fun FormField.defaultAnswer(): FormAnswer? = when (this) {
    is FormField.Text -> default?.let(FormAnswer::Text)
    is FormField.Toggle -> default?.let(FormAnswer::Bool)
    is FormField.MultiSelect -> default.takeIf { it.isNotEmpty() }?.let(FormAnswer::Choices)
    is FormField.Number -> default?.let(FormAnswer::Number)
    is FormField.External -> null
}

/** Starting answers: hidden presets plus visible defaults. */
fun Form.initialAnswers(): Map<String, FormAnswer> =
    presetAnswers + fields.mapNotNull { field -> field.defaultAnswer()?.let { field.key to it } }

/** The reply for [answers]: presets plus answers of fields that are currently visible. */
fun Form.reply(answers: Map<String, FormAnswer>): Map<String, FormAnswer> =
    presetAnswers + fields.filter { it.isVisible(answers) }.mapNotNull { field -> answers[field.key]?.let { field.key to it } }

/** Whether every visible required field has a non-empty answer. */
fun Form.isComplete(answers: Map<String, FormAnswer>): Boolean =
    fields.filter { it.required && it.isVisible(answers) }.all { field ->
        when (val answer = answers[field.key]) {
            null -> false
            is FormAnswer.Text -> answer.value.isNotBlank()
            is FormAnswer.Choices -> answer.values.isNotEmpty()
            is FormAnswer.Bool, is FormAnswer.Number -> true
        }
    }

/** A file sent with a prompt: a `data:` URI for uploads, `file://` for files on the server. */
data class PromptAttachment(
    val uri: String,
    val name: String,
    val mime: String? = null,
    /** The `@path` text of a file mention inside the prompt, when the file was mentioned. */
    val mention: String? = null,
)

/** A slash command defined on the server. */
data class Command(val name: String, val description: String? = null)
