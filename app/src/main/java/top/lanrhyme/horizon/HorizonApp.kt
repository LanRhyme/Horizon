package top.lanrhyme.horizon

import android.app.Application
import org.mozilla.geckoview.GeckoRuntime

class HorizonApp : Application() {

    lateinit var geckoRuntime: GeckoRuntime
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            val processName = getProcessName()
            if (processName != packageName) {
                // GeckoView 子进程 (gpu / tab / crashhelper 等) 绝不执行主进程运行时初始化
                return
            }
        }

        val settings = org.mozilla.geckoview.GeckoRuntimeSettings.Builder()
            .consoleOutput(true)
            .build()
        geckoRuntime = GeckoRuntime.create(this, settings)
    }

    companion object {
        lateinit var instance: HorizonApp
            private set
    }
}
