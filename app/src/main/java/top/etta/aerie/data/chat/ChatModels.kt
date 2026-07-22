package top.etta.aerie.data.chat

import kotlinx.coroutines.flow.Flow

data class ChatMessage(
    val accountId: String,
    val messageId: String,
    val messageOrder: Long,
    val conversationId: String,
    val turnId: String?,
    val role: String,
    val content: String,
    val attachmentsJson: String,
    val createdAt: String,
)

data class ChatRequestRecord(
    val accountId: String,
    val requestId: String,
    val conversationId: String?,
    val clientRequestId: String?,
    val status: String,
    val errorCode: String?,
    val retryOfRequestId: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val completedAt: String?,
)

enum class PendingOutboundState(val storedValue: String) {
    AwaitingConfirmation("awaiting_confirmation"),
    Sending("sending"),
}

data class PendingOutbound(
    val accountId: String,
    val clientRequestId: String,
    val text: String,
    val state: PendingOutboundState,
    val createdAt: String,
    val lastErrorCode: String? = null,
)

data class ChatSyncCursor(
    val accountId: String,
    val latestMessageId: String?,
    val lastEventId: String?,
    val syncedAt: String,
)

enum class ChatConnectionStatus {
    Idle,
    Connecting,
    Connected,
    Reconnecting,
    Offline,
}

data class ChatConnectionState(
    val accountId: String? = null,
    val status: ChatConnectionStatus = ChatConnectionStatus.Idle,
    val retryDelaySeconds: Int? = null,
    val errorCode: String? = null,
)

sealed interface ChatOperationResult {
    data class Success(val request: ChatRequestRecord? = null) : ChatOperationResult
    data class AwaitingConfirmation(val clientRequestId: String) : ChatOperationResult
    data class Failure(val code: String, val message: String) : ChatOperationResult
}

interface ChatLocalStore {
    fun observeMessages(accountId: String): Flow<List<ChatMessage>>
    fun observeRequests(accountId: String): Flow<List<ChatRequestRecord>>
    fun observePending(accountId: String): Flow<List<PendingOutbound>>

    suspend fun upsertMessages(messages: List<ChatMessage>)
    suspend fun upsertRequest(request: ChatRequestRecord)
    suspend fun activeRequests(accountId: String): List<ChatRequestRecord>
    suspend fun getPending(accountId: String, clientRequestId: String): PendingOutbound?
    suspend fun upsertPending(pending: PendingOutbound)
    suspend fun deletePending(accountId: String, clientRequestId: String)
    suspend fun recoverInterruptedSends(accountId: String)
    suspend fun getCursor(accountId: String): ChatSyncCursor?
    suspend fun upsertCursor(cursor: ChatSyncCursor)
    suspend fun clearCursor(accountId: String)
}

interface ChatRepository {
    fun observeMessages(accountId: String): Flow<List<ChatMessage>>
    fun observeRequests(accountId: String): Flow<List<ChatRequestRecord>>
    fun observePending(accountId: String): Flow<List<PendingOutbound>>
    fun observeConnection(accountId: String): Flow<ChatConnectionState>

    suspend fun synchronize(accountId: String): ChatOperationResult
    suspend fun runEventStream(accountId: String)
    suspend fun submit(accountId: String, text: String): ChatOperationResult
    suspend fun confirmPending(
        accountId: String,
        clientRequestId: String,
    ): ChatOperationResult
    suspend fun cancel(accountId: String, requestId: String): ChatOperationResult
    suspend fun retry(accountId: String, requestId: String): ChatOperationResult
}
