package top.etta.aerie.data.remote

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class ServerUrlPolicy(
    private val allowLocalHttp: Boolean,
) {
    fun normalize(rawUrl: String): String {
        val parsed = rawUrl.trim().toHttpUrlOrNull()
            ?: throw IllegalArgumentException("服务器地址格式无效")
        val localDebugHost = parsed.host in setOf("127.0.0.1", "localhost", "10.0.2.2")
        if (!parsed.isHttps && !(allowLocalHttp && localDebugHost)) {
            throw IllegalArgumentException("服务器必须使用 HTTPS")
        }
        if (parsed.username.isNotEmpty() || parsed.password.isNotEmpty()) {
            throw IllegalArgumentException("服务器地址不能包含账号信息")
        }
        if (parsed.query != null || parsed.fragment != null) {
            throw IllegalArgumentException("服务器地址不能包含查询参数或片段")
        }
        return parsed.newBuilder()
            .encodedPath("/")
            .query(null)
            .fragment(null)
            .build()
            .toString()
    }
}

class MobileApiFactory(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    },
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build(),
) {
    fun create(baseUrl: String): MobileAuthApi = retrofit(baseUrl)
        .create(MobileAuthApi::class.java)

    fun createChat(baseUrl: String): MobileChatApi = retrofit(baseUrl)
        .create(MobileChatApi::class.java)

    private fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}

data class MobileClientFailure(
    val code: String,
    val message: String,
    val requestId: String? = null,
)

class MobileApiErrorMapper(
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun map(error: Throwable): MobileClientFailure {
        if (error is HttpException) {
            val body = error.response()?.errorBody()?.string()
            val parsed = body?.let {
                runCatching { json.decodeFromString<ApiErrorEnvelopeDto>(it) }.getOrNull()
            }
            if (parsed != null) {
                return MobileClientFailure(
                    code = parsed.error.code,
                    message = displayMessage(parsed.error.code, parsed.error.message),
                    requestId = parsed.error.requestId,
                )
            }
            return MobileClientFailure(
                code = if (error.code() == 401) "invalid_token" else "backend_error",
                message = if (error.code() == 401) "登录信息无效或已过期" else "服务器请求失败",
            )
        }
        if (error is IOException) {
            return MobileClientFailure("network_unavailable", "无法连接服务器，请检查网络")
        }
        return MobileClientFailure("client_error", "客户端处理请求失败")
    }

    private fun displayMessage(code: String, serverMessage: String): String = when (code) {
        "invalid_credentials" -> "用户名、密码或配对码无效"
        "rate_limited" -> "尝试次数过多，请稍后再试"
        "invalid_token", "token_expired", "device_revoked" -> "登录信息无效或已过期"
        "account_disabled" -> "账号已被停用"
        "backend_unavailable", "chat_unavailable", "service_unavailable" -> "电脑端服务尚未就绪"
        else -> serverMessage.takeIf { it.isNotBlank() } ?: "服务器请求失败"
    }
}
