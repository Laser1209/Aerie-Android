package top.etta.aerie.data.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import top.etta.aerie.BuildConfig

enum class UserRole {
    Owner,
    Guest,
}

data class ActiveSession(
    val accountId: String,
    val deviceId: String,
    val displayName: String,
    val role: UserRole,
    val isLocalPreview: Boolean,
)

sealed interface SessionState {
    data object SignedOut : SessionState
    data class SignedIn(val session: ActiveSession) : SessionState
}

data class LoginInput(
    val serverUrl: String,
    val username: String,
    val password: String,
    val pairingCode: String,
    val deviceName: String,
)

sealed interface LoginResult {
    data object Success : LoginResult
    data class Failure(
        val code: String,
        val message: String,
    ) : LoginResult
}

interface SessionRepository {
    val session: StateFlow<SessionState>
    val accessToken: StateFlow<String?>
    val serverUrl: StateFlow<String?>

    suspend fun login(input: LoginInput): LoginResult
    suspend fun restoreSession(): Boolean
    suspend fun refreshAccessToken(staleAccessToken: String? = null): String?
    fun enterLocalPreview(role: UserRole)
    suspend fun logout()
}

class InMemorySessionRepository : SessionRepository {
    private val mutableSession = MutableStateFlow<SessionState>(SessionState.SignedOut)
    override val session: StateFlow<SessionState> = mutableSession.asStateFlow()
    private val mutableAccessToken = MutableStateFlow<String?>(null)
    override val accessToken: StateFlow<String?> = mutableAccessToken.asStateFlow()
    private val mutableServerUrl = MutableStateFlow<String?>(null)
    override val serverUrl: StateFlow<String?> = mutableServerUrl.asStateFlow()

    override suspend fun login(input: LoginInput): LoginResult {
        if (input.username.length !in 3..32) {
            return LoginResult.Failure("invalid_username", "用户名长度应为 3 到 32 个字符")
        }
        if (input.password.length < 12) {
            return LoginResult.Failure("invalid_password", "密码至少需要 12 个字符")
        }
        if (!input.pairingCode.matches(Regex("^[0-9]{8}$"))) {
            return LoginResult.Failure("invalid_pairing_code", "请输入八位配对码")
        }
        return LoginResult.Failure("backend_unavailable", "服务器认证接口尚未配置")
    }

    override suspend fun refreshAccessToken(staleAccessToken: String?): String? = null

    override suspend fun restoreSession(): Boolean = false

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
        mutableAccessToken.value = null
        mutableServerUrl.value = null
        mutableSession.value = SessionState.SignedOut
    }
}
