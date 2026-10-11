package top.lanrhyme.horizon.extension

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtensionController

data class CuratedExtension(
    val id: String,
    val name: String,
    val description: String,
    val downloadUrl: String,
    val author: String,
    val iconCategory: String = "tool"
)

data class InstalledExtensionItem(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val isEnabled: Boolean,
    val optionsUrl: String?,
    val raw: WebExtension
)

data class ExtensionPopupState(
    val extension: WebExtension,
    val session: GeckoSession,
    val title: String
)

class ExtensionManager(
    private val context: Context,
    private val runtime: GeckoRuntime
) : ViewModel() {

    private val controller: WebExtensionController = runtime.webExtensionController

    private val _installedExtensions = MutableStateFlow<List<InstalledExtensionItem>>(emptyList())
    val installedExtensions: StateFlow<List<InstalledExtensionItem>> = _installedExtensions.asStateFlow()

    private val _isInstalling = MutableStateFlow(false)
    val isInstalling: StateFlow<Boolean> = _isInstalling.asStateFlow()

    private val _installMessage = MutableStateFlow<String?>(null)
    val installMessage: StateFlow<String?> = _installMessage.asStateFlow()

    // 扩展弹窗 Popup 状态
    private val _popupState = MutableStateFlow<ExtensionPopupState?>(null)
    val popupState: StateFlow<ExtensionPopupState?> = _popupState.asStateFlow()

    // 精选扩展商店数据
    val curatedExtensions = listOf(
        CuratedExtension(
            id = "uBlock0@raymondhill.net",
            name = "uBlock Origin",
            description = "高效、精简的全能广告过滤与内容拦截神器",
            downloadUrl = "https://addons.mozilla.org/firefox/downloads/latest/ublock-origin/latest.xpi",
            author = "Raymond Hill"
        ),
        CuratedExtension(
            id = "addon@darkreader.org",
            name = "Dark Reader",
            description = "为所有网站生成优雅温和的夜间与莫兰迪暗色模式",
            downloadUrl = "https://addons.mozilla.org/firefox/downloads/latest/darkreader/latest.xpi",
            author = "Dark Reader"
        ),
        CuratedExtension(
            id = "violentmonkey@violentmonkey.github.io",
            name = "Violentmonkey 暴力猴",
            description = "轻量级开源用户脚本管理器，扩展网页无尽可能",
            downloadUrl = "https://addons.mozilla.org/firefox/downloads/latest/violentmonkey/latest.xpi",
            author = "Violentmonkey"
        ),
        CuratedExtension(
            id = "sponsorBlocker@ajay.app",
            name = "SponsorBlock",
            description = "自动跳过视频内嵌广告与赞助片段的社区驱动插件",
            downloadUrl = "https://addons.mozilla.org/firefox/downloads/latest/sponsorblock/latest.xpi",
            author = "Ajay Ramachandran"
        ),
        CuratedExtension(
            id = "{446900e4-71c2-419f-a6a7-df9c091e268b}",
            name = "Bitwarden 密码管理器",
            description = "安全便捷的跨平台开源密码管理解决方案",
            downloadUrl = "https://addons.mozilla.org/firefox/downloads/latest/bitwarden-password-manager/latest.xpi",
            author = "Bitwarden"
        )
    )

    init {
        // 设置安装权限提示处理器，默认放行
        controller.promptDelegate = object : WebExtensionController.PromptDelegate {
            override fun onInstallPromptRequest(
                extension: WebExtension,
                permissions: Array<String>,
                origins: Array<String>,
                dataCollectionPermissions: Array<String>
            ): GeckoResult<WebExtension.PermissionPromptResponse?> {
                val response = WebExtension.PermissionPromptResponse(
                    /* isPermissionsGranted = */ true,
                    /* isPrivateModeGranted = */ true,
                    /* isTechnicalAndInteractionDataGranted = */ true
                )
                return GeckoResult.fromValue(response)
            }
        }

        refreshInstalled()
    }

    fun refreshInstalled() {
        controller.list().accept(
            { list ->
                val items = list?.map { ext ->
                    setupExtensionActionDelegate(ext)
                    InstalledExtensionItem(
                        id = ext.id,
                        name = ext.metaData.name ?: ext.id,
                        version = ext.metaData.version ?: "1.0",
                        description = ext.metaData.description ?: "",
                        isEnabled = ext.metaData.enabled,
                        optionsUrl = ext.metaData.optionsPageUrl,
                        raw = ext
                    )
                } ?: emptyList()
                Handler(Looper.getMainLooper()).post {
                    _installedExtensions.value = items
                }
            },
            { error ->
                _installMessage.value = "获取已安装插件失败: ${error?.localizedMessage}"
            }
        )
    }

    private fun setupExtensionActionDelegate(extension: WebExtension) {
        extension.setActionDelegate(object : WebExtension.ActionDelegate {
            override fun onOpenPopup(
                ext: WebExtension,
                action: WebExtension.Action
            ): GeckoResult<GeckoSession> {
                val result = GeckoResult<GeckoSession>()
                Handler(Looper.getMainLooper()).post {
                    val popupSession = GeckoSession().apply {
                        open(runtime)
                    }
                    _popupState.value = ExtensionPopupState(
                        extension = ext,
                        session = popupSession,
                        title = action.title ?: ext.metaData.name ?: "扩展弹窗"
                    )
                    result.complete(popupSession)
                }
                return result
            }

            override fun onTogglePopup(
                ext: WebExtension,
                action: WebExtension.Action
            ): GeckoResult<GeckoSession> {
                if (_popupState.value != null && _popupState.value?.extension?.id == ext.id) {
                    closePopup()
                    return GeckoResult.fromValue(null)
                }
                return onOpenPopup(ext, action)
            }
        })
    }

    fun installExtension(urlOrUri: String) {
        _isInstalling.value = true
        _installMessage.value = "正在下载并安装扩展..."
        controller.install(urlOrUri).accept(
            { installedExt ->
                Handler(Looper.getMainLooper()).post {
                    _isInstalling.value = false
                    _installMessage.value = "扩展「${installedExt?.metaData?.name ?: "插件"}」安装成功"
                    refreshInstalled()
                }
            },
            { error ->
                Handler(Looper.getMainLooper()).post {
                    _isInstalling.value = false
                    _installMessage.value = "安装失败: ${error?.localizedMessage}"
                }
            }
        )
    }

    fun toggleExtension(item: InstalledExtensionItem) {
        val targetState = !item.isEnabled
        val result = if (targetState) {
            controller.enable(item.raw, WebExtensionController.EnableSource.USER)
        } else {
            controller.disable(item.raw, WebExtensionController.EnableSource.USER)
        }
        result.accept({
            refreshInstalled()
        }, { error ->
            _installMessage.value = "切换状态失败: ${error?.localizedMessage}"
        })
    }

    fun uninstallExtension(item: InstalledExtensionItem) {
        controller.uninstall(item.raw).accept({
            refreshInstalled()
            _installMessage.value = "已卸载「${item.name}」"
        }, { error ->
            _installMessage.value = "卸载失败: ${error?.localizedMessage}"
        })
    }

    fun closePopup() {
        val current = _popupState.value
        if (current != null) {
            current.session.close()
            _popupState.value = null
        }
    }

    fun clearMessage() {
        _installMessage.value = null
    }
}
