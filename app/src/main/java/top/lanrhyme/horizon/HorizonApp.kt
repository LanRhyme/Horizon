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

        // 注册 Mozilla Components 日志输出到 Android Logcat
        try {
            mozilla.components.support.base.log.Log.addSink(
                mozilla.components.support.base.log.sink.AndroidLogSink("HorizonMoz")
            )
            mozilla.components.support.base.log.Log.logLevel = mozilla.components.support.base.log.Log.Priority.DEBUG
        } catch (_: Exception) {}

        // 初始化本地存储、下载中心与跨端同步管理器
        top.lanrhyme.horizon.core.BookmarkHistoryManager.init(this)
        top.lanrhyme.horizon.core.DownloadManager.init(this)
        top.lanrhyme.horizon.sync.FxaSyncManager.init(this)
    }

    companion object {
        lateinit var instance: HorizonApp
            private set
    }
}
