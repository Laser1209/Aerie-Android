package top.etta.aerie.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class MobileMessageDto(
    val messageId: String,
    val messageOrder: Long,
    val conversationId: String,
    val turnId: String? = null,
    val role: String,
    val content: String,
    val attachments: List<JsonElement> = emptyList(),
    val createdAt: String,
)

@Serializable
data class MobileMessagePageDto(
    val items: List<MobileMessageDto>,
    val hasMore: Boolean,
)

@Serializable
data class SubmitMobileRequestDto(
    val clientRequestId: String,
    val text: String,
    val fileIds: List<String> = emptyList(),
)

@Serializable
data class MobileRequestDto(
    val requestId: String,
    val conversationId: String? = null,
    val status: String,
    val clientRequestId: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val completedAt: String? = null,
    val errorCode: String? = null,
    val retryOfRequestId: String? = null,
)

interface MobileChatApi {
    @GET("api/mobile/v1/messages")
    suspend fun messages(
        @Header("Authorization") authorization: String,
        @Query("beforeId") beforeId: String? = null,
        @Query("afterId") afterId: String? = null,
        @Query("limit") limit: Int = 100,
    ): MobileMessagePageDto

    @POST("api/mobile/v1/requests")
    suspend fun submitRequest(
        @Header("Authorization") authorization: String,
        @Body body: SubmitMobileRequestDto,
    ): MobileRequestDto

    @GET("api/mobile/v1/requests/{requestId}")
    suspend fun request(
        @Header("Authorization") authorization: String,
        @Path("requestId") requestId: String,
    ): MobileRequestDto

    @POST("api/mobile/v1/requests/{requestId}/cancel")
    suspend fun cancelRequest(
        @Header("Authorization") authorization: String,
        @Path("requestId") requestId: String,
    ): MobileRequestDto

    @POST("api/mobile/v1/requests/{requestId}/retry")
    suspend fun retryRequest(
        @Header("Authorization") authorization: String,
        @Path("requestId") requestId: String,
    ): MobileRequestDto
}
