package top.lanrhyme.horizon

import android.app.Application
import org.mozilla.geckoview.GeckoRuntime

class HorizonApp : Application() {

    lateinit var geckoRuntime: GeckoRuntime
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        val settings = org.mozilla.geckoview.GeckoRuntimeSettings.Builder()
            .isolatedProcessEnabled(false)
            .fissionEnabled(false)
            .consoleOutput(true)
            .build()
        geckoRuntime = GeckoRuntime.create(this, settings)
    }

    companion object {
        lateinit var instance: HorizonApp
            private set
    }
}
