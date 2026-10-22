package io.github.beppe2880.opencodemobile.feature.terminal

import androidx.compose.runtime.Immutable

/** Keys a phone keyboard lacks; each maps to a libvterm key. */
enum class SpecialKey(val id: String, val label: String) {
    Escape("esc", "Esc"),
    Tab("tab", "Tab"),
    Enter("enter", "↵"),
    Backspace("bksp", "⌫"),
    Left("left", "←"),
    Down("down", "↓"),
    Up("up", "↑"),
    Right("right", "→"),
    Home("home", "Home"),
    End("end", "End"),
    PageUp("pgup", "PgUp"),
    PageDown("pgdn", "PgDn"),
    Insert("ins", "Ins"),
    Delete("del", "Del"),
    F1("f1", "F1"),
    F2("f2", "F2"),
    F3("f3", "F3"),
    F4("f4", "F4"),
    F5("f5", "F5"),
    F6("f6", "F6"),
    F7("f7", "F7"),
    F8("f8", "F8"),
    F9("f9", "F9"),
    F10("f10", "F10"),
    F11("f11", "F11"),
    F12("f12", "F12"),
}

enum class TermModifier(val id: String, val label: String, val mask: Int) {
    Shift("shift", "Shift", 0x01),
    Alt("alt", "Alt", 0x02),
    Ctrl("ctrl", "Ctrl", 0x04),
}

/** Off; on for the next key only; on until tapped again. */
enum class ModifierState { Off, Once, Locked }

/** Tapping a modifier arms it for one key, a second tap locks it, a third releases it. */
fun ModifierState.next(): ModifierState = when (this) {
    ModifierState.Off -> ModifierState.Once
    ModifierState.Once -> ModifierState.Locked
    ModifierState.Locked -> ModifierState.Off
}

/** A key of the bar, stored in settings as its [spec]. */
@Immutable
sealed interface BarKey {
    val spec: String
    val label: String

    data class Special(val key: SpecialKey) : BarKey {
        override val spec get() = key.id
        override val label get() = key.label
    }

    data class Modifier(val modifier: TermModifier) : BarKey {
        override val spec get() = modifier.id
        override val label get() = modifier.label
    }

    data object Paste : BarKey {
        override val spec get() = "paste"
        override val label get() = "Paste"
    }

    /** Sends [text] as typed: one character takes the active modifiers, longer text is a macro. */
    data class Text(override val label: String, val text: String) : BarKey {
        override val spec get() = if (label == text) "char:$text" else "text:$label$SEPARATOR$text"
    }

    companion object {
        private const val SEPARATOR = '\u001f'

        fun char(char: Char) = Text(char.toString(), char.toString())

        fun parse(spec: String): BarKey? {
            SpecialKey.entries.firstOrNull { it.id == spec }?.let { return Special(it) }
            TermModifier.entries.firstOrNull { it.id == spec }?.let { return Modifier(it) }
            return when {
                spec == Paste.spec -> Paste
                spec.startsWith("char:") -> spec.removePrefix("char:").takeIf { it.isNotEmpty() }?.let { Text(it, it) }
                spec.startsWith("text:") -> spec.removePrefix("text:").split(SEPARATOR, limit = 2)
                    .takeIf { it.size == 2 && it.all(String::isNotEmpty) }
                    ?.let { (label, text) -> Text(label, text) }
                else -> null
            }
        }
    }
}

object BarKeys {
    /** Characters that are slow to reach on a phone keyboard. */
    private const val SYMBOLS = "|~/\\`-_*&\$><{}[]()=+;:'\"#%^@!?"

    val Default: List<List<BarKey>> = listOf(
        listOf(
            BarKey.Special(SpecialKey.Escape),
            BarKey.Special(SpecialKey.Tab),
            BarKey.Paste,
            BarKey.char('/'),
            BarKey.Special(SpecialKey.Home),
            BarKey.Special(SpecialKey.Up),
            BarKey.Special(SpecialKey.End),
            BarKey.Special(SpecialKey.PageUp),
        ),
        listOf(
            BarKey.Modifier(TermModifier.Shift),
            BarKey.Modifier(TermModifier.Ctrl),
            BarKey.Modifier(TermModifier.Alt),
            BarKey.Special(SpecialKey.Left),
            BarKey.Special(SpecialKey.Down),
            BarKey.Special(SpecialKey.Right),
            BarKey.Special(SpecialKey.PageDown),
            BarKey.char('|'),
            BarKey.char('~'),
            BarKey.char('\\'),
            BarKey.char('`'),
        ),
    )

    /** Everything the editor offers to add. */
    val Catalog: List<BarKey> =
        TermModifier.entries.map(BarKey::Modifier) +
            SpecialKey.entries.map(BarKey::Special) +
            BarKey.Paste +
            SYMBOLS.map(BarKey::char)

    fun fromSettings(rows: List<List<String>>?): List<List<BarKey>> =
        rows?.map { row -> row.mapNotNull(BarKey::parse) }?.takeIf { it.size == ROWS } ?: Default

    fun toSettings(rows: List<List<BarKey>>): List<List<String>> = rows.map { row -> row.map { it.spec } }

    const val ROWS = 2
}

/**
 * Turns what the user typed for a custom key into the text it sends: `\e` is Escape, `\n` and
 * `\r` new line and return, `\t` tab, `\\` a backslash and `^X` the Ctrl combination of X.
 */
fun decodeKeyText(typed: String): String = buildString {
    var i = 0
    while (i < typed.length) {
        val c = typed[i]
        val next = typed.getOrNull(i + 1)
        when {
            c == '\\' && next != null -> {
                append(
                    when (next) {
                        'e' -> '\u001b'
                        'n' -> '\n'
                        'r' -> '\r'
                        't' -> '\t'
                        else -> next
                    },
                )
                i += 2
            }
            c == '^' && next != null && next.uppercaseChar() in '@'..'_' -> {
                append((next.uppercaseChar().code - '@'.code).toChar())
                i += 2
            }
            else -> {
                append(c)
                i++
            }
        }
    }
}
