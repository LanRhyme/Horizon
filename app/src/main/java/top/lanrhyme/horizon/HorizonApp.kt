package top.lanrhyme.horizon

import android.app.Application
import org.mozilla.geckoview.GeckoRuntime

class HorizonApp : Application() {

    lateinit var geckoRuntime: GeckoRuntime
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        geckoRuntime = GeckoRuntime.create(this)
    }

    companion object {
        lateinit var instance: HorizonApp
            private set
    }
}
