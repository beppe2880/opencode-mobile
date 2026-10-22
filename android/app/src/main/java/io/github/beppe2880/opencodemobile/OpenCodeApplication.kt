package io.github.beppe2880.opencodemobile

import android.app.Application
import io.github.beppe2880.opencodemobile.core.data.di.AppScope
import io.github.beppe2880.opencodemobile.notifications.AndroidMonitorNotifier
import io.github.beppe2880.opencodemobile.notifications.SessionMonitor
import io.github.beppe2880.opencodemobile.widget.WidgetUpdater
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class OpenCodeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val koin = startKoin {
            androidContext(this@OpenCodeApplication)
            modules(dataModules() + appModule)
        }.koin
        koin.get<AndroidMonitorNotifier>().createChannels()
        // Idle until session notifications are enabled in Settings.
        koin.get<SessionMonitor>().start(koin.get(AppScope))
        koin.get<WidgetUpdater>().start(koin.get(AppScope))
    }
}
