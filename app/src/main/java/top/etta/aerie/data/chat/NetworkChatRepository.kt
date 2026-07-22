package top.etta.aerie.data.chat

import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import top.etta.aerie.data.remote.AuthorizedRequestExecutor
import top.etta.aerie.data.remote.MobileApiErrorMapper
import top.etta.aerie.data.remote.MobileChatApi
import top.etta.aerie.data.remote.MobileClientFailure
import top.etta.aerie.data.remote.MobileMessageDto
import top.etta.aerie.data.remote.MobileRequestDto
import top.etta.aerie.data.remote.SessionUnavailableException
import top.etta.aerie.data.remote.SubmitMobileRequestDto
import top.etta.aerie.data.session.SessionRepository
import top.etta.aerie.data.session.SessionState

class NetworkChatRepository(
    private val sessionRepository: SessionRepository,
    private val authorizedExecutor: AuthorizedRequestExecutor,
    private val apiFactory: (String) -> MobileChatApi,
    private val localStore: ChatLocalStore,
    private val errorMapper: MobileApiErrorMapper = MobileApiErrorMapper(),
    private val json: Json = Json,
    private val newClientRequestId: () -> String = { UUID.randomUUID().toString() },
    private val now: () -> String = { Instant.now().toString() },
) : ChatRepository {
    private val syncMutex = Mutex()
    private var activeApiUrl: String? = null
    private var activeApi: MobileChatApi? = null

    override fun observeMessages(accountId: String): Flow<List<ChatMessage>> =
        localStore.observeMessages(accountId)

    override fun observeRequests(accountId: String): Flow<List<ChatRequestRecord>> =
        localStore.observeRequests(accountId)

    override fun observePending(accountId: String): Flow<List<PendingOutbound>> =
        localStore.observePending(accountId)

    override suspend fun synchronize(accountId: String): ChatOperationResult = syncMutex.withLock {
        try {
            requireActiveAccount(accountId)
            localStore.recoverInterruptedSends(accountId)
            synchronizeMessages(accountId)
            synchronizeActiveRequests(accountId)
            ChatOperationResult.Success()
        } catch (error: CancellationException) {
            throw error
        } catch (error: MappedChatException) {
            ChatOperationResult.Failure(error.failure.code, error.failure.message)
        } catch (error: Throwable) {
            error.toOperationFailure()
        }
    }

    override suspend fun submit(accountId: String, text: String): ChatOperationResult {
        val content = text.trim()
        if (content.isEmpty()) {
            return ChatOperationResult.Failure("empty_request", "消息不能为空")
        }
        if (content.length > 20_000) {
            return ChatOperationResult.Failure("text_too_long", "消息不能超过 20000 字符")
        }
        val pending = PendingOutbound(
            accountId = accountId,
            clientRequestId = newClientRequestId(),
            text = content,
            state = PendingOutboundState.Sending,
            createdAt = now(),
        )
        localStore.upsertPending(pending)
        return sendPending(pending)
    }

    override suspend fun confirmPending(
        accountId: String,
        clientRequestId: String,
    ): ChatOperationResult {
        val pending = localStore.getPending(accountId, clientRequestId)
            ?: return ChatOperationResult.Failure("pending_not_found", "待发送消息不存在")
        val sending = pending.copy(
            state = PendingOutboundState.Sending,
            lastErrorCode = null,
        )
        localStore.upsertPending(sending)
        return sendPending(sending)
    }

    override suspend fun cancel(
        accountId: String,
        requestId: String,
    ): ChatOperationResult = performRequestAction(accountId) { api, authorization ->
        api.cancelRequest(authorization, requestId)
    }

    override suspend fun retry(
        accountId: String,
        requestId: String,
    ): ChatOperationResult = performRequestAction(accountId) { api, authorization ->
        api.retryRequest(authorization, requestId)
    }

    private suspend fun sendPending(pending: PendingOutbound): ChatOperationResult {
        return try {
            requireActiveAccount(pending.accountId)
            val response = authorizedCall { api, authorization ->
                api.submitRequest(
                    authorization,
                    SubmitMobileRequestDto(
                        clientRequestId = pending.clientRequestId,
                        text = pending.text,
                    ),
                )
            }
            val request = response.toDomain(
                accountId = pending.accountId,
                fallbackClientRequestId = pending.clientRequestId,
                fallbackCreatedAt = pending.createdAt,
            )
            localStore.upsertRequest(request)
            localStore.deletePending(pending.accountId, pending.clientRequestId)
            ChatOperationResult.Success(request)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            val failure = error.toClientFailure()
            if (failure.code in UNCERTAIN_DELIVERY_ERRORS) {
                localStore.upsertPending(
                    pending.copy(
                        state = PendingOutboundState.AwaitingConfirmation,
                        lastErrorCode = failure.code,
                    ),
                )
                ChatOperationResult.AwaitingConfirmation(pending.clientRequestId)
            } else {
                localStore.deletePending(pending.accountId, pending.clientRequestId)
                ChatOperationResult.Failure(failure.code, failure.message)
            }
        }
    }

    private suspend fun performRequestAction(
        accountId: String,
        action: suspend (MobileChatApi, String) -> MobileRequestDto,
    ): ChatOperationResult = try {
        requireActiveAccount(accountId)
        val response = authorizedCall(action)
        val request = response.toDomain(accountId)
        localStore.upsertRequest(request)
        ChatOperationResult.Success(request)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        error.toOperationFailure()
    }

    private suspend fun synchronizeMessages(accountId: String) {
        val cursor = localStore.getCursor(accountId)
        if (cursor?.latestMessageId == null) {
            fullMessageSync(accountId, cursor)
            return
        }
        try {
            incrementalMessageSync(accountId, cursor)
        } catch (error: Throwable) {
            val failure = error.toClientFailure()
            if (failure.code != "invalid_cursor") throw MappedChatException(failure)
            localStore.clearCursor(accountId)
            fullMessageSync(accountId, null)
        }
    }

    private suspend fun fullMessageSync(
        accountId: String,
        previousCursor: ChatSyncCursor?,
    ) {
        var page = authorizedCall { api, authorization ->
            api.messages(authorization = authorization, limit = PAGE_SIZE)
        }
        localStore.upsertMessages(page.items.map { it.toDomain(accountId) })
        val latestMessageId = page.items.lastOrNull()?.messageId
        var oldestMessageId = page.items.firstOrNull()?.messageId
        var pageCount = 1
        while (page.hasMore && oldestMessageId != null && pageCount < MAX_SYNC_PAGES) {
            page = authorizedCall { api, authorization ->
                api.messages(
                    authorization = authorization,
                    beforeId = oldestMessageId,
                    limit = PAGE_SIZE,
                )
            }
            if (page.items.isEmpty()) break
            localStore.upsertMessages(page.items.map { it.toDomain(accountId) })
            val nextOldest = page.items.first().messageId
            if (nextOldest == oldestMessageId) break
            oldestMessageId = nextOldest
            pageCount += 1
        }
        localStore.upsertCursor(
            ChatSyncCursor(
                accountId = accountId,
                latestMessageId = latestMessageId,
                lastEventId = previousCursor?.lastEventId,
                syncedAt = now(),
            ),
        )
    }

    private suspend fun incrementalMessageSync(
        accountId: String,
        cursor: ChatSyncCursor,
    ) {
        var latestMessageId = cursor.latestMessageId
        var pageCount = 0
        do {
            val page = authorizedCall { api, authorization ->
                api.messages(
                    authorization = authorization,
                    afterId = latestMessageId,
                    limit = PAGE_SIZE,
                )
            }
            if (page.items.isEmpty()) break
            localStore.upsertMessages(page.items.map { it.toDomain(accountId) })
            val nextLatest = page.items.last().messageId
            if (nextLatest == latestMessageId) break
            latestMessageId = nextLatest
            pageCount += 1
        } while (page.hasMore && pageCount < MAX_SYNC_PAGES)
        localStore.upsertCursor(cursor.copy(latestMessageId = latestMessageId, syncedAt = now()))
    }

    private suspend fun synchronizeActiveRequests(accountId: String) {
        localStore.activeRequests(accountId).forEach { existing ->
            val response = authorizedCall { api, authorization ->
                api.request(authorization, existing.requestId)
            }
            localStore.upsertRequest(response.toDomain(accountId))
        }
    }

    private suspend fun <T> authorizedCall(
        call: suspend (MobileChatApi, String) -> T,
    ): T = authorizedExecutor.execute { accessToken ->
        call(api(), "Bearer $accessToken")
    }

    private fun api(): MobileChatApi {
        val serverUrl = sessionRepository.serverUrl.value ?: throw SessionUnavailableException()
        if (activeApi == null || activeApiUrl != serverUrl) {
            activeApi = apiFactory(serverUrl)
            activeApiUrl = serverUrl
        }
        return checkNotNull(activeApi)
    }

    private fun requireActiveAccount(accountId: String) {
        val signedIn = sessionRepository.session.value as? SessionState.SignedIn
            ?: throw SessionUnavailableException()
        if (signedIn.session.accountId != accountId || signedIn.session.isLocalPreview) {
            throw SessionUnavailableException()
        }
    }

    private fun Throwable.toClientFailure(): MobileClientFailure =
        if (this is SessionUnavailableException) {
            MobileClientFailure("invalid_token", "登录信息无效或已过期")
        } else {
            errorMapper.map(this)
        }

    private fun Throwable.toOperationFailure(): ChatOperationResult.Failure {
        val failure = toClientFailure()
        return ChatOperationResult.Failure(failure.code, failure.message)
    }

    private fun MobileMessageDto.toDomain(accountId: String) = ChatMessage(
        accountId = accountId,
        messageId = messageId,
        conversationId = conversationId,
        turnId = turnId,
        role = role,
        content = content,
        attachmentsJson = json.encodeToString(attachments),
        createdAt = createdAt,
    )

    private fun MobileRequestDto.toDomain(
        accountId: String,
        fallbackClientRequestId: String? = null,
        fallbackCreatedAt: String? = null,
    ) = ChatRequestRecord(
        accountId = accountId,
        requestId = requestId,
        conversationId = conversationId,
        clientRequestId = clientRequestId ?: fallbackClientRequestId,
        status = status,
        errorCode = errorCode,
        retryOfRequestId = retryOfRequestId,
        createdAt = createdAt ?: fallbackCreatedAt,
        updatedAt = updatedAt,
        completedAt = completedAt,
    )

    private class MappedChatException(val failure: MobileClientFailure) : Exception()

    private companion object {
        const val PAGE_SIZE = 100
        const val MAX_SYNC_PAGES = 100
        val UNCERTAIN_DELIVERY_ERRORS = setOf(
            "network_unavailable",
            "backend_unavailable",
            "chat_unavailable",
            "service_unavailable",
        )
    }
}
