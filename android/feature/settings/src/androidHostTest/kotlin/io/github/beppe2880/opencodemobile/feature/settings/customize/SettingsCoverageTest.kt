package io.github.beppe2880.opencodemobile.feature.settings.customize

import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Test
import java.lang.reflect.Modifier

class SettingsCoverageTest {

    /** A new [AppSettings] property fails here until an option (and so a preview part) changes it. */
    @Test
    fun `every setting can be changed from a preview`() {
        val properties = AppSettings::class.java.declaredFields
            .filterNot { Modifier.isStatic(it.modifiers) }
            .map { it.name }
            .toSet()
        val covered = SettingOption.entries.flatMap { it.fields }.toSet()
        assertEquals(emptySet<String>(), properties - covered - UNLISTED_FIELDS)
        // No option names a property that does not exist.
        assertEquals(emptySet<String>(), covered + UNLISTED_FIELDS - properties)
    }
}
