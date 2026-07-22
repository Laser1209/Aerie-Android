package top.etta.aerie.data.session

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import top.etta.aerie.BuildConfig
import top.etta.aerie.data.remote.LoginRequestDto
import top.etta.aerie.data.remote.MobileApiErrorMapper
import top.etta.aerie.data.remote.MobileAuthApi
import top.etta.aerie.data.remote.RefreshRequestDto
import top.etta.aerie.data.remote.ServerUrlPolicy
import top.etta.aerie.data.remote.TokenResponseDto
import top.etta.aerie.data.security.SecureSessionStore
import top.etta.aerie.data.security.StoredSession

class NetworkSessionRepository(
    private val apiFactory: (String) -> MobileAuthApi,
    private val secureStore: SecureSessionStore,
    private val serverUrlPolicy: ServerUrlPolicy,
    private val errorMapper: MobileApiErrorMapper = MobileApiErrorMapper(),
) : SessionRepository {
    private val mutableSession = MutableStateFlow<SessionState>(SessionState.SignedOut)
    override val session: StateFlow<SessionState> = mutableSession.asStateFlow()

    private val mutableAccessToken = MutableStateFlow<String?>(null)
    override val accessToken: StateFlow<String?> = mutableAccessToken.asStateFlow()

    private val mutableServerUrl = MutableStateFlow<String?>(null)
    override val serverUrl: StateFlow<String?> = mutableServerUrl.asStateFlow()

    private val authMutex = Mutex()
    private var activeApi: MobileAuthApi? = null

    override suspend fun login(input: LoginInput): LoginResult {
        validate(input)?.let { return it }
        val serverUrl = try {
            serverUrlPolicy.normalize(input.serverUrl)
        } catch (error: IllegalArgumentException) {
            return LoginResult.Failure("invalid_server_url", error.message ?: "服务器地址无效")
        }
        return authMutex.withLock {
            val api = apiFactory(serverUrl)
            try {
                val response = api.login(
                    LoginRequestDto(
                        username = input.username.trim().lowercase(),
                        password = input.password,
                        deviceName = input.deviceName.trim(),
                        pairingCode = input.pairingCode,
                    ),
                )
                applyTokenResponse(serverUrl, api, response)
                LoginResult.Success
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                val failure = errorMapper.map(error)
                LoginResult.Failure(failure.code, failure.message)
            }
        }
    }

    override suspend fun restoreSession(): Boolean = refreshAccessToken() != null

    override suspend fun refreshAccessToken(staleAccessToken: String?): String? = authMutex.withLock {
        val current = mutableAccessToken.value
        if (staleAccessToken == null && current != null) {
            return@withLock current
        }
        if (staleAccessToken != null && current != null && current != staleAccessToken) {
            return@withLock current
        }
        val stored = try {
            secureStore.read()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            return@withLock null
        } ?: return@withLock null
        val api = activeApi ?: apiFactory(stored.serverUrl)
        try {
            val response = api.refresh(RefreshRequestDto(stored.refreshToken))
            if (response.account.accountId != stored.accountId ||
                response.account.deviceId != stored.deviceId
            ) {
                clearLocalSession()
                return@withLock null
            }
            applyTokenResponse(stored.serverUrl, api, response)
            response.accessToken
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            val failure = errorMapper.map(error)
            if (failure.code in TERMINAL_SESSION_ERRORS) {
                clearLocalSession()
            }
            null
        }
    }

    override fun enterLocalPreview(role: UserRole) {
        check(BuildConfig.ALLOW_LOCAL_PREVIEW) {
            "Local preview is disabled for this build"
        }
        mutableSession.value = SessionState.SignedIn(
            ActiveSession(
                accountId = "preview",
                deviceId = "preview",
                displayName = if (role == UserRole.Owner) "Owner Preview" else "Guest Preview",
                role = role,
                isLocalPreview = true,
            ),
        )
    }

    override suspend fun logout() {
        authMutex.withLock {
            val token = mutableAccessToken.value
            val api = activeApi
            try {
                if (token != null && api != null) {
                    api.logout("Bearer $token")
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                // Local logout remains authoritative when the server is unreachable.
            } finally {
                clearLocalSession()
            }
        }
    }

    private suspend fun applyTokenResponse(
        serverUrl: String,
        api: MobileAuthApi,
        response: TokenResponseDto,
    ) {
        val role = when (response.account.role.lowercase()) {
            "owner" -> UserRole.Owner
            "guest" -> UserRole.Guest
            else -> error("server returned an unsupported role")
        }
        secureStore.write(
            StoredSession(
                serverUrl = serverUrl,
                refreshToken = response.refreshToken,
                accountId = response.account.accountId,
                deviceId = response.account.deviceId,
                username = response.account.username,
                role = response.account.role,
            ),
        )
        activeApi = api
        mutableServerUrl.value = serverUrl
        mutableAccessToken.value = response.accessToken
        mutableSession.value = SessionState.SignedIn(
            ActiveSession(
                accountId = response.account.accountId,
                deviceId = response.account.deviceId,
                displayName = response.account.username,
                role = role,
                isLocalPreview = false,
            ),
        )
    }

    private suspend fun clearLocalSession() {
        secureStore.clear()
        activeApi = null
        mutableServerUrl.value = null
        mutableAccessToken.value = null
        mutableSession.value = SessionState.SignedOut
    }

    private fun validate(input: LoginInput): LoginResult.Failure? = when {
        !input.username.matches(Regex("^[A-Za-z0-9._-]{3,32}$")) ->
            LoginResult.Failure("invalid_username", "用户名格式无效")
        input.password.length < 12 ->
            LoginResult.Failure("invalid_password", "密码至少需要 12 个字符")
        !input.pairingCode.matches(Regex("^[0-9]{8}$")) ->
            LoginResult.Failure("invalid_pairing_code", "请输入八位配对码")
        input.deviceName.trim().isEmpty() ->
            LoginResult.Failure("invalid_device_name", "设备名称不能为空")
        else -> null
    }

    private companion object {
        val TERMINAL_SESSION_ERRORS = setOf(
            "invalid_token",
            "token_expired",
            "device_revoked",
            "account_disabled",
        )
    }
}
