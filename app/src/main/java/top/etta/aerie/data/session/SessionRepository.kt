package top.etta.aerie.data.session

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import top.etta.aerie.BuildConfig

enum class UserRole {
    Owner,
    Guest,
}

data class ActiveSession(
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
    data class Failure(val message: String) : LoginResult
}

interface SessionRepository {
    val session: StateFlow<SessionState>

    suspend fun login(input: LoginInput): LoginResult
    fun enterLocalPreview(role: UserRole)
    fun logout()
}

class InMemorySessionRepository : SessionRepository {
    private val mutableSession = MutableStateFlow<SessionState>(SessionState.SignedOut)
    override val session: StateFlow<SessionState> = mutableSession.asStateFlow()

    override suspend fun login(input: LoginInput): LoginResult {
        if (input.username.length !in 3..32) {
            return LoginResult.Failure("用户名长度应为 3 到 32 个字符")
        }
        if (input.password.length < 12) {
            return LoginResult.Failure("密码至少需要 12 个字符")
        }
        if (!input.pairingCode.matches(Regex("^[0-9]{8}$"))) {
            return LoginResult.Failure("请输入八位配对码")
        }
        delay(350)
        return LoginResult.Failure("服务器认证接口尚未通过 Phase 2 验收")
    }

    override fun enterLocalPreview(role: UserRole) {
        check(BuildConfig.ALLOW_LOCAL_PREVIEW) {
            "Local preview is disabled for this build"
        }
        mutableSession.value = SessionState.SignedIn(
            ActiveSession(
                displayName = if (role == UserRole.Owner) "Owner Preview" else "Guest Preview",
                role = role,
                isLocalPreview = true,
            ),
        )
    }

    override fun logout() {
        mutableSession.value = SessionState.SignedOut
    }
}
