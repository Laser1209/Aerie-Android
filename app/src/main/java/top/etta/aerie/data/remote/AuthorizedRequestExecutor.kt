package top.etta.aerie.data.remote

import retrofit2.HttpException
import top.etta.aerie.data.session.SessionRepository

class SessionUnavailableException : IllegalStateException("authenticated session is unavailable")

class AuthorizedRequestExecutor(
    private val sessionRepository: SessionRepository,
) {
    suspend fun <T> execute(call: suspend (accessToken: String) -> T): T {
        val initialToken = sessionRepository.accessToken.value
            ?: sessionRepository.refreshAccessToken()
            ?: throw SessionUnavailableException()
        return try {
            call(initialToken)
        } catch (error: HttpException) {
            if (error.code() != 401) throw error
            val refreshed = sessionRepository.refreshAccessToken(initialToken)
                ?: throw SessionUnavailableException()
            call(refreshed)
        }
    }
}
