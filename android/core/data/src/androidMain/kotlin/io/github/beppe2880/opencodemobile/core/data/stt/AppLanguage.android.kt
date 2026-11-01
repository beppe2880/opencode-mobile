package io.github.beppe2880.opencodemobile.core.data.stt

import java.util.Locale

// The per-app language picked in the system settings is the process default.
internal actual fun appLanguage(): String? = Locale.getDefault().toLanguageTag()
