package top.lanrhyme.horizon.sync

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mozilla.appservices.fxaclient.FxaConfig
import mozilla.appservices.fxaclient.FxaServer
import mozilla.components.browser.storage.sync.RemoteTabsStorage
import mozilla.components.concept.base.crash.Breadcrumb
import mozilla.components.concept.base.crash.CrashReporting
import mozilla.components.concept.sync.*
import mozilla.components.service.fxa.FxaAuthData
import mozilla.components.service.fxa.manager.FxaAccountManager
import mozilla.components.service.fxa.manager.ext.withConstellationIfExists
import org.json.JSONArray
import org.json.JSONObject
import top.lanrhyme.horizon.core.BookmarkHistoryManager
import top.lanrhyme.horizon.model.BookmarkItem

data class RemoteTab(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val faviconUrl: String? = null,
    val lastActiveTime: Long = System.currentTimeMillis()
)

data class RemoteDevice(
    val id: String,
    val name: String,
    val deviceType: String = "desktop", // desktop, mobile, tablet
    val lastSyncTime: String,
    val tabs: List<RemoteTab> = emptyList()
)

data class SyncAccountState(
    val isLoggedIn: Boolean = false,
    val userEmail: String? = null,
    val avatarUrl: String? = null,
    val isSyncing: Boolean = false,
    val lastSyncTime: String? = null,
    val syncBookmarksEnabled: Boolean = true,
    val syncHistoryEnabled: Boolean = true,
    val syncTabsEnabled: Boolean = true,
    val syncPasswordsEnabled: Boolean = true,
    val autoSyncEnabled: Boolean = true,
    val remoteDevices: List<RemoteDevice> = emptyList(),
    val authUrl: String? = null
)

object FxaSyncManager {

    private const val PREFS_NAME = "horizon_fxa_sync"
    private const val KEY_SYNC_DATA = "fxa_account_data"
    const val FXA_CLIENT_ID = "a2270f727f45f648"
    const val REDIRECT_URI = "https://accounts.firefox.com/oauth/success/a2270f727f45f648"

    private var appContext: Context? = null
    private var prefs: SharedPreferences? = null

    private var accountManager: FxaAccountManager? = null
    private var tabsStorage: RemoteTabsStorage? = null

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var autoSyncJob: Job? = null

    private val _accountState = MutableStateFlow(SyncAccountState())
    val accountState: StateFlow<SyncAccountState> = _accountState.asStateFlow()

    private object NoOpCrashReporting : CrashReporting {
        override fun submitCaughtException(throwable: Throwable): Job = Job()
        override fun recordCrashBreadcrumb(breadcrumb: Breadcrumb) {}
    }

    fun init(context: Context) {
        val app = context.applicationContext
        appContext = app
        prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        loadLocalAccountState()

        val serverConfig = FxaConfig(
            server = FxaServer.Release,
            clientId = FXA_CLIENT_ID,
            redirectUri = REDIRECT_URI,
            tokenServerUrlOverride = null
        )
        val deviceConfig = DeviceConfig(
            name = "OnePlus Pad 2 Pro (Horizon)",
            type = DeviceType.TABLET,
            capabilities = emptySet(),
            secureStateAtRest = false
        )
        val syncConfig = SyncConfig(
            supportedEngines = setOf(SyncEngine.Tabs, SyncEngine.Bookmarks, SyncEngine.History),
            periodicSyncConfig = PeriodicSyncConfig(periodMinutes = 15, initialDelayMinutes = 5)
        )

        accountManager = FxaAccountManager(
            context = app,
            serverConfig = serverConfig,
            deviceConfig = deviceConfig,
            syncConfig = syncConfig,
            applicationScopes = setOf("profile", "https://identity.mozilla.com/apps/oldsync"),
            crashReporter = NoOpCrashReporting,
            coroutineContext = Dispatchers.IO
        ).also { manager ->
            manager.register(object : AccountObserver {
                override fun onProfileUpdated(profile: Profile) {
                    scope.launch {
                        _accountState.value = _accountState.value.copy(
                            isLoggedIn = true,
                            userEmail = profile.email,
                            avatarUrl = profile.avatar?.url
                        )
                        saveLocalAccountState()
                    }
                }

                override fun onLoggedOut() {
                    scope.launch {
                        _accountState.value = SyncAccountState()
                        saveLocalAccountState()
                    }
                }

                override fun onAuthenticationProblems() {}
            })
        }

        tabsStorage = RemoteTabsStorage(app, crashReporter = NoOpCrashReporting)

        // 启动 FxA 状态机
        scope.launch(Dispatchers.IO) {
            try {
                accountManager?.start()
                val connected = accountManager?.authenticatedAccount()
                if (connected != null) {
                    val profile = accountManager?.accountProfile()
                    withContext(Dispatchers.Main) {
                        _accountState.value = _accountState.value.copy(
                            isLoggedIn = true,
                            userEmail = profile?.email ?: _accountState.value.userEmail,
                            avatarUrl = profile?.avatar?.url ?: _accountState.value.avatarUrl
                        )
                        saveLocalAccountState()
                    }
                    triggerSync()
                }
            } catch (e: Exception) {
                android.util.Log.e("HorizonFxa", "Error starting FxaAccountManager", e)
            }
        }

        startAutoSyncSchedule()
    }

    private fun startAutoSyncSchedule() {
        autoSyncJob?.cancel()
        autoSyncJob = scope.launch {
            while (isActive) {
                if (_accountState.value.isLoggedIn && _accountState.value.autoSyncEnabled) {
                    triggerSync()
                }
                delay(15 * 60 * 1000L)
            }
        }
    }

    private fun loadLocalAccountState() {
        val sp = prefs ?: return
        val raw = sp.getString(KEY_SYNC_DATA, null) ?: return
        try {
            val json = JSONObject(raw)
            val isLogged = json.optBoolean("isLoggedIn", false)
            _accountState.value = SyncAccountState(
                isLoggedIn = isLogged,
                userEmail = if (json.has("userEmail")) json.optString("userEmail") else null,
                avatarUrl = if (json.has("avatarUrl")) json.optString("avatarUrl") else null,
                lastSyncTime = if (json.has("lastSyncTime")) json.optString("lastSyncTime") else null,
                syncBookmarksEnabled = json.optBoolean("syncBookmarksEnabled", true),
                syncHistoryEnabled = json.optBoolean("syncHistoryEnabled", true),
                syncTabsEnabled = json.optBoolean("syncTabsEnabled", true),
                syncPasswordsEnabled = json.optBoolean("syncPasswordsEnabled", true),
                autoSyncEnabled = json.optBoolean("autoSyncEnabled", true),
                remoteDevices = if (isLogged) loadCachedDevices(json) else emptyList()
            )
        } catch (_: Exception) {}
    }

    private fun loadCachedDevices(json: JSONObject): List<RemoteDevice> {
        val list = mutableListOf<RemoteDevice>()
        val arr = json.optJSONArray("remoteDevices") ?: return list
        for (i in 0 until arr.length()) {
            val dObj = arr.getJSONObject(i)
            val tabsArr = dObj.optJSONArray("tabs") ?: JSONArray()
            val tabs = mutableListOf<RemoteTab>()
            for (j in 0 until tabsArr.length()) {
                val tObj = tabsArr.getJSONObject(j)
                tabs.add(
                    RemoteTab(
                        id = tObj.optString("id", java.util.UUID.randomUUID().toString()),
                        title = tObj.getString("title"),
                        url = tObj.getString("url"),
                        faviconUrl = tObj.optString("faviconUrl", null),
                        lastActiveTime = tObj.optLong("lastActiveTime", System.currentTimeMillis())
                    )
                )
            }
            list.add(
                RemoteDevice(
                    id = dObj.getString("id"),
                    name = dObj.getString("name"),
                    deviceType = dObj.optString("deviceType", "desktop"),
                    lastSyncTime = dObj.optString("lastSyncTime", "刚刚"),
                    tabs = tabs
                )
            )
        }
        return list
    }

    private fun saveLocalAccountState() {
        val sp = prefs ?: return
        val state = _accountState.value
        val json = JSONObject().apply {
            put("isLoggedIn", state.isLoggedIn)
            if (state.userEmail != null) put("userEmail", state.userEmail)
            if (state.avatarUrl != null) put("avatarUrl", state.avatarUrl)
            if (state.lastSyncTime != null) put("lastSyncTime", state.lastSyncTime)
            put("syncBookmarksEnabled", state.syncBookmarksEnabled)
            put("syncHistoryEnabled", state.syncHistoryEnabled)
            put("syncTabsEnabled", state.syncTabsEnabled)
            put("syncPasswordsEnabled", state.syncPasswordsEnabled)
            put("autoSyncEnabled", state.autoSyncEnabled)

            val devArr = JSONArray()
            state.remoteDevices.forEach { dev ->
                val dObj = JSONObject().apply {
                    put("id", dev.id)
                    put("name", dev.name)
                    put("deviceType", dev.deviceType)
                    put("lastSyncTime", dev.lastSyncTime)
                    val tArr = JSONArray()
                    dev.tabs.forEach { t ->
                        tArr.put(JSONObject().apply {
                            put("id", t.id)
                            put("title", t.title)
                            put("url", t.url)
                            if (t.faviconUrl != null) put("faviconUrl", t.faviconUrl)
                            put("lastActiveTime", t.lastActiveTime)
                        })
                    }
                    put("tabs", tArr)
                }
                devArr.put(dObj)
            }
            put("remoteDevices", devArr)
        }
        sp.edit().putString(KEY_SYNC_DATA, json.toString()).apply()
    }

    // 启动官方 FxA OAuth 登录流程
    fun requestAuthUrl(onUrlReady: (String) -> Unit) {
        val manager = accountManager
        if (manager == null) {
            android.util.Log.w("HorizonFxa", "accountManager is null, using fallback url")
            onUrlReady(getFallbackAuthUrl())
            return
        }
        scope.launch(Dispatchers.IO) {
            try {
                android.util.Log.d("HorizonFxa", "Calling manager.beginAuthentication...")
                val entryPoint = object : FxAEntryPoint {
                    override val entryName: String = "settings"
                }
                val authUrl = manager.beginAuthentication(
                    entrypoint = entryPoint
                )
                android.util.Log.d("HorizonFxa", "beginAuthentication result: $authUrl")
                withContext(Dispatchers.Main) {
                    if (!authUrl.isNullOrBlank()) {
                        onUrlReady(authUrl)
                    } else {
                        android.util.Log.w("HorizonFxa", "beginAuthentication returned null or blank, using fallback")
                        onUrlReady(getFallbackAuthUrl())
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("HorizonFxa", "Error in beginAuthentication", e)
                withContext(Dispatchers.Main) {
                    onUrlReady(getFallbackAuthUrl())
                }
            }
        }
    }

    private fun getFallbackAuthUrl(): String {
        // 生成标准 PKCE code_verifier & code_challenge
        val verifierBytes = ByteArray(32)
        java.security.SecureRandom().nextBytes(verifierBytes)
        val verifier = android.util.Base64.encodeToString(verifierBytes, android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP)

        val md = java.security.MessageDigest.getInstance("SHA-256")
        val challengeBytes = md.digest(verifier.toByteArray(Charsets.US_ASCII))
        val challenge = android.util.Base64.encodeToString(challengeBytes, android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP)

        val stateBytes = ByteArray(16)
        java.security.SecureRandom().nextBytes(stateBytes)
        val state = android.util.Base64.encodeToString(stateBytes, android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP)

        // 生成标准 EC P-256 JWK
        val kpg = java.security.KeyPairGenerator.getInstance("EC")
        kpg.initialize(java.security.spec.ECGenParameterSpec("secp256r1"))
        val kp = kpg.generateKeyPair()
        val pub = kp.public as java.security.interfaces.ECPublicKey
        val w = pub.w

        fun bigIntToBytes(bi: java.math.BigInteger): ByteArray {
            val raw = bi.toByteArray()
            return when {
                raw.size == 32 -> raw
                raw.size > 32 -> raw.copyOfRange(raw.size - 32, raw.size)
                else -> ByteArray(32).apply { System.arraycopy(raw, 0, this, 32 - raw.size, raw.size) }
            }
        }

        val xB64 = android.util.Base64.encodeToString(bigIntToBytes(w.affineX), android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP)
        val yB64 = android.util.Base64.encodeToString(bigIntToBytes(w.affineY), android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP)

        val jwkJson = """{"kty":"EC","crv":"P-256","x":"$xB64","y":"$yB64"}"""
        val keysJwk = android.util.Base64.encodeToString(jwkJson.toByteArray(Charsets.UTF_8), android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP)

        // 缓存 fallback PKCE 凭据
        prefs?.edit()
            ?.putString("fallback_code_verifier", verifier)
            ?.putString("fallback_state", state)
            ?.apply()

        val uriBuilder = Uri.parse("https://accounts.firefox.com/authorization").buildUpon()
            .appendQueryParameter("action", "email")
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("entrypoint", "settings")
            .appendQueryParameter("client_id", FXA_CLIENT_ID)
            .appendQueryParameter("state", state)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("access_type", "offline")
            .appendQueryParameter("keys_jwk", keysJwk)
            .appendQueryParameter("scope", "profile https://identity.mozilla.com/apps/oldsync")
            .appendQueryParameter("redirect_uri", REDIRECT_URI)

        return uriBuilder.build().toString()
    }

    // 处理 OAuth 回调参数 (code, state) 或回调 Uri
    fun handleAuthCallback(uriString: String, onComplete: (Boolean) -> Unit) {
        val manager = accountManager
        scope.launch(Dispatchers.IO) {
            try {
                val uri = Uri.parse(uriString)
                val code = uri.getQueryParameter("code")
                val state = uri.getQueryParameter("state")

                if (code.isNullOrBlank() || state.isNullOrBlank()) {
                    android.util.Log.w("HorizonFxa", "Callback missing code or state: $uriString")
                    withContext(Dispatchers.Main) { onComplete(false) }
                    return@launch
                }

                android.util.Log.d("HorizonFxa", "Handling OAuth callback: code=$code, state=$state")

                // 优先尝试使用 FxaAccountManager.finishAuthentication
                var success = false
                if (manager != null) {
                    try {
                        val authData = FxaAuthData(
                            authType = AuthType.Signin,
                            code = code,
                            state = state,
                            declinedEngines = emptySet()
                        )
                        success = manager.finishAuthentication(authData)
                        android.util.Log.d("HorizonFxa", "manager.finishAuthentication result: $success")
                    } catch (e: Exception) {
                        android.util.Log.w("HorizonFxa", "manager.finishAuthentication error, trying token exchange fallback", e)
                    }
                }

                // 如果 manager 处于 Disconnected 或未知状态导致 finishAuthentication 失败，通过标准 PKCE 端点置换 Token
                if (!success) {
                    val verifier = prefs?.getString("fallback_code_verifier", null) ?: ""
                    android.util.Log.d("HorizonFxa", "Attempting fallback token exchange with verifier length: ${verifier.length}")

                    val tokenUrl = "https://oauth.accounts.firefox.com/v1/token"
                    val payload = JSONObject().apply {
                        put("client_id", FXA_CLIENT_ID)
                        put("code", code)
                        put("code_verifier", verifier)
                        put("grant_type", "authorization_code")
                    }

                    val conn = (java.net.URL(tokenUrl).openConnection() as java.net.HttpURLConnection).apply {
                        requestMethod = "POST"
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json")
                        setRequestProperty("Accept", "application/json")
                        connectTimeout = 15000
                        readTimeout = 15000
                    }

                    conn.outputStream.use { os ->
                        os.write(payload.toString().toByteArray(Charsets.UTF_8))
                        os.flush()
                    }

                    val responseCode = conn.responseCode
                    android.util.Log.d("HorizonFxa", "Token exchange HTTP response: $responseCode")
                    if (responseCode in 200..299) {
                        val respText = conn.inputStream.bufferedReader().use { it.readText() }
                        val respJson = JSONObject(respText)
                        val accessToken = respJson.optString("access_token", null)
                        val refreshToken = respJson.optString("refresh_token", null)

                        // 获取个人资料 (Profile)
                        var userEmail = "已连接 Firefox 账户"
                        var avatarUrl: String? = null
                        if (!accessToken.isNullOrBlank()) {
                            try {
                                val profConn = (java.net.URL("https://profile.accounts.firefox.com/v1/profile").openConnection() as java.net.HttpURLConnection).apply {
                                    requestMethod = "GET"
                                    setRequestProperty("Authorization", "Bearer $accessToken")
                                    connectTimeout = 10000
                                    readTimeout = 10000
                                }
                                if (profConn.responseCode in 200..299) {
                                    val profText = profConn.inputStream.bufferedReader().use { it.readText() }
                                    val profJson = JSONObject(profText)
                                    userEmail = profJson.optString("email", userEmail)
                                    avatarUrl = profJson.optString("avatar", null)
                                }
                            } catch (e: Exception) {
                                android.util.Log.w("HorizonFxa", "Error fetching profile", e)
                            }
                        }

                        withContext(Dispatchers.Main) {
                            _accountState.value = _accountState.value.copy(
                                isLoggedIn = true,
                                userEmail = userEmail,
                                avatarUrl = avatarUrl
                            )
                            saveLocalAccountState()
                            onComplete(true)
                        }
                        triggerSync()
                        return@launch
                    } else {
                        val errText = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                        android.util.Log.e("HorizonFxa", "Token exchange error ($responseCode): $errText")
                    }
                }

                if (success) {
                    val profile = manager?.accountProfile()
                    withContext(Dispatchers.Main) {
                        _accountState.value = _accountState.value.copy(
                            isLoggedIn = true,
                            userEmail = profile?.email ?: "已连接 Firefox 账户",
                            avatarUrl = profile?.avatar?.url
                        )
                        saveLocalAccountState()
                        onComplete(true)
                    }
                    triggerSync()
                } else {
                    withContext(Dispatchers.Main) { onComplete(false) }
                }
            } catch (e: Exception) {
                android.util.Log.e("HorizonFxa", "Error in handleAuthCallback", e)
                withContext(Dispatchers.Main) { onComplete(false) }
            }
        }
    }

    fun logout() {
        scope.launch(Dispatchers.IO) {
            try {
                accountManager?.logout()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            withContext(Dispatchers.Main) {
                _accountState.value = SyncAccountState()
                saveLocalAccountState()
            }
        }
    }

    fun triggerSync() {
        val manager = accountManager ?: return
        if (!_accountState.value.isLoggedIn) return
        _accountState.value = _accountState.value.copy(isSyncing = true)

        scope.launch(Dispatchers.IO) {
            try {
                // 1. 触发 Sync 核心引擎同步
                manager.syncNow(mozilla.components.service.fxa.sync.SyncReason.User)

                // 2. 从 Rust RemoteTabsStorage 读取同步到的多端设备与标签页
                val devicesMap = tabsStorage?.getAll() ?: emptyMap()
                val parsedDevices = mutableListOf<RemoteDevice>()

                devicesMap.forEach { (client, tabsList) ->
                    val remoteTabs = tabsList.mapNotNull { tab ->
                        val entry = tab.active()
                        if (entry != null && entry.url.isNotBlank()) {
                            RemoteTab(
                                title = entry.title ?: entry.url,
                                url = entry.url,
                                faviconUrl = entry.iconUrl,
                                lastActiveTime = tab.lastUsed
                            )
                        } else null
                    }

                    if (remoteTabs.isNotEmpty()) {
                        parsedDevices.add(
                            RemoteDevice(
                                id = client.id,
                                name = client.id,
                                deviceType = "desktop",
                                lastSyncTime = "刚刚",
                                tabs = remoteTabs
                            )
                        )
                    }
                }

                // 3. 同时查询已注册设备星系 (Device Constellation) 完善设备名与设备类型
                manager.withConstellationIfExists {
                    val constellationState = this.state()
                    val otherDevices = constellationState?.otherDevices ?: emptyList()
                    otherDevices.forEach { dev ->
                        val existingIndex = parsedDevices.indexOfFirst { it.id == dev.id }
                        val typeStr = when (dev.deviceType) {
                            DeviceType.MOBILE -> "mobile"
                            DeviceType.TABLET -> "tablet"
                            else -> "desktop"
                        }
                        if (existingIndex >= 0) {
                            parsedDevices[existingIndex] = parsedDevices[existingIndex].copy(
                                name = dev.displayName,
                                deviceType = typeStr
                            )
                        } else {
                            parsedDevices.add(
                                RemoteDevice(
                                    id = dev.id,
                                    name = dev.displayName,
                                    deviceType = typeStr,
                                    lastSyncTime = dev.lastAccessTime?.let {
                                        java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it))
                                    } ?: "刚刚",
                                    tabs = emptyList()
                                )
                            )
                        }
                    }
                }

                val now = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                withContext(Dispatchers.Main) {
                    _accountState.value = _accountState.value.copy(
                        isSyncing = false,
                        lastSyncTime = now,
                        remoteDevices = if (parsedDevices.isNotEmpty()) parsedDevices else _accountState.value.remoteDevices
                    )
                    saveLocalAccountState()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _accountState.value = _accountState.value.copy(isSyncing = false)
                }
            }
        }
    }

    fun toggleSyncBookmarks(enabled: Boolean) {
        _accountState.value = _accountState.value.copy(syncBookmarksEnabled = enabled)
        saveLocalAccountState()
    }

    fun toggleSyncHistory(enabled: Boolean) {
        _accountState.value = _accountState.value.copy(syncHistoryEnabled = enabled)
        saveLocalAccountState()
    }

    fun toggleSyncTabs(enabled: Boolean) {
        _accountState.value = _accountState.value.copy(syncTabsEnabled = enabled)
        saveLocalAccountState()
    }

    fun toggleSyncPasswords(enabled: Boolean) {
        _accountState.value = _accountState.value.copy(syncPasswordsEnabled = enabled)
        saveLocalAccountState()
    }

    fun toggleAutoSync(enabled: Boolean) {
        _accountState.value = _accountState.value.copy(autoSyncEnabled = enabled)
        saveLocalAccountState()
    }
}
