package io.github.beppe2880.opencodemobile.fake

import android.content.Context
import androidx.lifecycle.ViewModel
import io.github.beppe2880.opencodemobile.appModule
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/** Keeps the isolated demo alive across rotation, but never across an exit or process restart. */
class DemoData : ViewModel() {
    private var application: KoinApplication? = null

    /**
     * The demo's container; with [realSettings] (the live demo's edit mode) the settings are the
     * user's own, so what is changed in the demo is kept, while everything else stays sample data.
     */
    fun container(context: Context, realSettings: SettingsRepository? = null): KoinApplication = application ?: koinApplication {
        androidContext(context.applicationContext)
        modules(fakeDataModule, appModule)
        if (realSettings != null) modules(module { single<SettingsRepository> { realSettings } })
    }.also { application = it }

    fun close() {
        application?.close()
        application = null
    }

    override fun onCleared() = close()
}
