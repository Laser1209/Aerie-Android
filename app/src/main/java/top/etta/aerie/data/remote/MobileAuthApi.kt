package top.etta.aerie.data.remote

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

@Serializable
data class LoginRequestDto(
    val username: String,
    val password: String,
    val deviceName: String,
    val pairingCode: String,
    val publicKey: String? = null,
)

@Serializable
data class RefreshRequestDto(
    val refreshToken: String,
)

@Serializable
data class TokenAccountDto(
    val accountId: String,
    val username: String,
    val role: String,
    val actorId: String,
    val userId: String,
    val deviceId: String,
)

@Serializable
data class TokenResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresIn: Long,
    val refreshExpiresIn: Long,
    val account: TokenAccountDto,
)

@Serializable
data class ApiErrorEnvelopeDto(
    val error: ApiErrorDto,
)

@Serializable
data class ApiErrorDto(
    val code: String,
    val message: String,
    val requestId: String,
)

interface MobileAuthApi {
    @POST("api/mobile/v1/auth/login")
    suspend fun login(@Body body: LoginRequestDto): TokenResponseDto

    @POST("api/mobile/v1/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequestDto): TokenResponseDto

    @POST("api/mobile/v1/auth/logout")
    suspend fun logout(@Header("Authorization") authorization: String): Response<Unit>
}
