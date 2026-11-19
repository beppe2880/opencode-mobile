package io.github.beppe2880.opencodemobile.core.ui.platform

/** Notification controls are hidden on platforms that do not deliver session notifications. */
expect val sessionNotificationsSupported: Boolean
