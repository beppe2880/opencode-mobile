package io.github.beppe2880.opencodemobile.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The 16 ANSI colors of the terminal: black, red, green, yellow, blue, magenta, cyan, white,
 * then their bright variants. The light set keeps every color readable on a light background,
 * where xterm's yellow and white disappear.
 */
val OcColors.terminalAnsi: List<Color>
    get() = if (isDark) DarkAnsi else LightAnsi

private val DarkAnsi = listOf(
    Color(0xFF000000), Color(0xFFCD3131), Color(0xFF0DBC79), Color(0xFFE5E510),
    Color(0xFF2472C8), Color(0xFFBC3FBC), Color(0xFF11A8CD), Color(0xFFE5E5E5),
    Color(0xFF666666), Color(0xFFF14C4C), Color(0xFF23D18B), Color(0xFFF5F543),
    Color(0xFF3B8EEA), Color(0xFFD670D6), Color(0xFF29B8DB), Color(0xFFFFFFFF),
)

private val LightAnsi = listOf(
    Color(0xFF24292F), Color(0xFFCF222E), Color(0xFF116329), Color(0xFF7D4E00),
    Color(0xFF0969DA), Color(0xFF8250DF), Color(0xFF1B7C83), Color(0xFF6E7781),
    Color(0xFF57606A), Color(0xFFA40E26), Color(0xFF1A7F37), Color(0xFF946300),
    Color(0xFF218BFF), Color(0xFFA475F9), Color(0xFF3192AA), Color(0xFF8C959F),
)
