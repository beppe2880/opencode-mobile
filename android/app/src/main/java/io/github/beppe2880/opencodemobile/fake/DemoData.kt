package io.github.beppe2880.opencodemobile.fake

import android.content.Context
import androidx.lifecycle.ViewModel
import io.github.beppe2880.opencodemobile.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication

/** Keeps the isolated demo alive across rotation, but never across an exit or process restart. */
class DemoData : ViewModel() {
    private var application: KoinApplication? = null

    fun container(context: Context): KoinApplication = application ?: koinApplication {
        androidContext(context.applicationContext)
        modules(fakeDataModule, appModule)
    }.also { application = it }

    fun close() {
        application?.close()
        application = null
    }

    override fun onCleared() = close()
}
