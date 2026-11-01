package io.github.beppe2880.opencodemobile.core.data.stt

import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages

// The per-app language picked in the system settings comes first.
internal actual fun appLanguage(): String? = NSLocale.preferredLanguages.firstOrNull() as? String
