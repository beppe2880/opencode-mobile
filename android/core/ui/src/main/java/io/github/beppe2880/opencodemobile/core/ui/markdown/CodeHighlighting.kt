package io.github.beppe2880.opencodemobile.core.ui.markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.BoldHighlight
import dev.snipme.highlights.model.ColorHighlight
import dev.snipme.highlights.model.SyntaxLanguage
import dev.snipme.highlights.model.SyntaxThemes

/**
 * [code] with syntax colors for the language of [fileName], in the theme of the chat's code
 * blocks. Prose and unknown file types, and code longer than [MAX_HIGHLIGHTED_CHARS], stay plain.
 * Slow on large files: call it off the main thread.
 */
fun highlightCode(code: String, fileName: String, dark: Boolean): AnnotatedString {
    val language = languageOf(fileName)
    if (language == null || code.length > MAX_HIGHLIGHTED_CHARS) return AnnotatedString(code)
    val highlights = Highlights.Builder()
        .code(code)
        .language(language)
        .theme(SyntaxThemes.atom(darkMode = dark))
        .build()
        .getHighlights()
    return buildAnnotatedString {
        append(code)
        for (highlight in highlights) {
            val start = highlight.location.start.coerceIn(0, code.length)
            val end = highlight.location.end.coerceIn(start, code.length)
            when (highlight) {
                is ColorHighlight -> addStyle(SpanStyle(color = Color(highlight.rgb).copy(alpha = 1f)), start, end)
                is BoldHighlight -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
            }
        }
    }
}

/** Highlighting is regex based and grows with the file; past this it would take seconds. */
const val MAX_HIGHLIGHTED_CHARS = 200_000

private fun languageOf(fileName: String): SyntaxLanguage? {
    val name = fileName.substringAfterLast('/').lowercase()
    if (name == "dockerfile" || name == "makefile") return SyntaxLanguage.SHELL
    return when (name.substringAfterLast('.', "")) {
        "kt", "kts" -> SyntaxLanguage.KOTLIN
        "java", "gradle", "groovy" -> SyntaxLanguage.JAVA
        "c", "h" -> SyntaxLanguage.C
        "cpp", "cc", "cxx", "hpp", "hh", "hxx", "m", "mm" -> SyntaxLanguage.CPP
        "dart" -> SyntaxLanguage.DART
        "rs" -> SyntaxLanguage.RUST
        "cs" -> SyntaxLanguage.CSHARP
        "coffee" -> SyntaxLanguage.COFFEESCRIPT
        "js", "mjs", "cjs", "jsx" -> SyntaxLanguage.JAVASCRIPT
        "ts", "tsx", "mts", "cts" -> SyntaxLanguage.TYPESCRIPT
        "pl", "pm" -> SyntaxLanguage.PERL
        "py", "pyi" -> SyntaxLanguage.PYTHON
        "rb" -> SyntaxLanguage.RUBY
        "sh", "bash", "zsh", "fish" -> SyntaxLanguage.SHELL
        "swift" -> SyntaxLanguage.SWIFT
        "go" -> SyntaxLanguage.GO
        "php" -> SyntaxLanguage.PHP
        // Strings, numbers and comments still stand out in data and markup files.
        "json", "jsonc", "yaml", "yml", "toml", "xml", "html", "css", "scss", "sql", "nix", "lua", "vue", "svelte", "proto" ->
            SyntaxLanguage.DEFAULT
        else -> null
    }
}
