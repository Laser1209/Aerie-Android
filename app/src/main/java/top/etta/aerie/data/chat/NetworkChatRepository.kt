package top.etta.aerie.data.chat

import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import top.etta.aerie.data.remote.AuthorizedRequestExecutor
import top.etta.aerie.data.remote.MobileApiErrorMapper
import top.etta.aerie.data.remote.MobileChatApi
import top.etta.aerie.data.remote.MobileClientFailure
import top.etta.aerie.data.remote.MobileEventStream
import top.etta.aerie.data.remote.MobileEventStreamResult
import top.etta.aerie.data.remote.MobileMessageDto
import top.etta.aerie.data.remote.MobileRequestDto
import top.etta.aerie.data.remote.MobileSseFrame
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
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    },
    private val newClientRequestId: () -> String = { UUID.randomUUID().toString() },
    private val now: () -> String = { Instant.now().toString() },
    private val eventStreamFactory: (String) -> MobileEventStream = {
        MobileEventStream(it, okhttp3.OkHttpClient())
    },
    private val sleep: suspend (Long) -> Unit = { delay(it) },
    random: () -> Double = { kotlin.random.Random.nextDouble() },
) : ChatRepository {
    private val syncMutex = Mutex()
    private val reconnectBackoff = SseReconnectBackoff(random)
    private val mutableConnectionState = MutableStateFlow(ChatConnectionState())
    private var activeApiUrl: String? = null
    private var activeApi: MobileChatApi? = null

    override fun observeMessages(accountId: String): Flow<List<ChatMessage>> =
        localStore.observeMessages(accountId)

    override fun observeRequests(accountId: String): Flow<List<ChatRequestRecord>> =
        localStore.observeRequests(accountId)

    override fun observePending(accountId: String): Flow<List<PendingOutbound>> =
        localStore.observePending(accountId)

    override fun observeConnection(accountId: String): Flow<ChatConnectionState> =
        mutableConnectionState.map { state ->
            if (state.accountId == null || state.accountId == accountId) {
                state.copy(accountId = accountId)
            } else {
                ChatConnectionState(accountId = accountId)
            }
        }

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

    override suspend fun runEventStream(accountId: String) {
        requireActiveAccount(accountId)
        var attempt = 0
        try {
            while (true) {
                currentCoroutineContext().ensureActive()
                var lastErrorCode: String? = null
                try {
                    requireActiveAccount(accountId)
                    publishConnection(accountId, ChatConnectionStatus.Connecting)
                    synchronizeOrThrow(accountId)

                    val serverUrl = sessionRepository.serverUrl.value
                        ?: throw SessionUnavailableException()
                    var authRetryUsed = false
                    var opened = false
                    var result: MobileEventStreamResult? = null
                    while (result == null) {
                        val accessToken = authorizedExecutor.execute { token -> token }
                        val connection = eventStreamFactory(serverUrl).connect(
                            accessToken = accessToken,
                            lastEventId = localStore.getCursor(accountId)?.lastEventId,
                            onOpen = {
                                opened = true
                                publishConnection(accountId, ChatConnectionStatus.Connected)
                            },
                            onFrame = { frame -> handleEvent(accountId, frame) },
                        )
                        if (connection is MobileEventStreamResult.Failed &&
                            connection.statusCode == 401 &&
                            !authRetryUsed
                        ) {
                            authRetryUsed = true
                            if (sessionRepository.refreshAccessToken(accessToken) == null) {
                                throw SessionUnavailableException()
                            }
                            continue
                        }
                        result = connection
                    }
                    val connectionResult = checkNotNull(result)
                    if (connectionResult is MobileEventStreamResult.Failed) {
                        val failure = connectionResult.cause?.toClientFailure()
                            ?: MobileClientFailure("network_unavailable", "无法连接服务器，请检查网络")
                        if (failure.code in TERMINAL_SESSION_ERRORS) {
                            publishConnection(accountId, ChatConnectionStatus.Offline, failure.code)
                            return
                        }
                        lastErrorCode = failure.code
                    }
                    if (opened) attempt = 0
                } catch (error: CancellationException) {
                    throw error
                } catch (error: SessionUnavailableException) {
                    publishConnection(accountId, ChatConnectionStatus.Offline, "invalid_token")
                    return
                } catch (error: ChatStreamException) {
                    if (error.failure.code in TERMINAL_SESSION_ERRORS) {
                        publishConnection(accountId, ChatConnectionStatus.Offline, error.failure.code)
                        return
                    }
                    lastErrorCode = error.failure.code
                } catch (error: Throwable) {
                    val failure = error.toClientFailure()
                    if (failure.code in TERMINAL_SESSION_ERRORS) {
                        publishConnection(accountId, ChatConnectionStatus.Offline, failure.code)
                        return
                    }
                    lastErrorCode = failure.code
                }

                val delayMillis = reconnectBackoff.delayMillis(attempt)
                publishConnection(
                    accountId = accountId,
                    status = ChatConnectionStatus.Reconnecting,
                    errorCode = lastErrorCode,
                    retryDelaySeconds = (delayMillis / 1_000L).coerceAtLeast(1L).toInt(),
                )
                sleep(delayMillis)
                attempt = (attempt + 1).coerceAtMost(4)
            }
        } finally {
            mutableConnectionState.value = ChatConnectionState()
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

    private suspend fun synchronizeOrThrow(accountId: String) {
        when (val result = synchronize(accountId)) {
            is ChatOperationResult.Success -> Unit
            is ChatOperationResult.Failure -> {
                throw ChatStreamException(
                    MobileClientFailure(result.code, result.message),
                )
            }
            is ChatOperationResult.AwaitingConfirmation -> Unit
        }
    }

    private suspend fun handleEvent(accountId: String, frame: MobileSseFrame) {
        syncMutex.withLock {
            val current = localStore.getCursor(accountId)
            val eventId = frame.id?.takeIf(::isValidEventId)
            when (frame.type) {
                "stream.open" -> return@withLock
                "message.created" -> {
                    val message = runCatching {
                        json.decodeFromString<MobileMessageDto>(frame.data)
                    }.getOrNull() ?: return@withLock
                    localStore.upsertMessages(listOf(message.toDomain(accountId)))
                    val nextMessageId = if (isAfter(current?.lastEventId, eventId)) {
                        message.messageId
                    } else {
                        current?.latestMessageId ?: message.messageId
                    }
                    localStore.upsertCursor(
                        nextCursor(accountId, current, eventId, nextMessageId),
                    )
                }
                "request.updated" -> {
                    val request = runCatching {
                        json.decodeFromString<MobileRequestDto>(frame.data)
                    }.getOrNull() ?: return@withLock
                    localStore.upsertRequest(request.toDomain(accountId))
                    localStore.upsertCursor(nextCursor(accountId, current, eventId))
                }
                "approval.pending", "file.updated" -> {
                    runCatching { json.parseToJsonElement(frame.data) }
                        .getOrNull() ?: return@withLock
                    localStore.upsertCursor(nextCursor(accountId, current, eventId))
                }
                else -> Unit
            }
        }
    }

    private fun nextCursor(
        accountId: String,
        current: ChatSyncCursor?,
        eventId: String?,
        latestMessageId: String? = current?.latestMessageId,
    ) = ChatSyncCursor(
        accountId = accountId,
        latestMessageId = latestMessageId,
        lastEventId = if (isAfter(current?.lastEventId, eventId)) eventId
        else current?.lastEventId ?: eventId,
        syncedAt = now(),
    )

    private fun isAfter(previous: String?, next: String?): Boolean {
        if (next == null) return false
        if (previous == null) return true
        return eventSequence(next) > eventSequence(previous)
    }

    private fun isValidEventId(id: String): Boolean =
        id.startsWith("evt_") && id.removePrefix("evt_").toLongOrNull() != null

    private fun eventSequence(id: String): Long =
        id.removePrefix("evt_").toLongOrNull() ?: Long.MIN_VALUE

    private fun publishConnection(
        accountId: String,
        status: ChatConnectionStatus,
        errorCode: String? = null,
        retryDelaySeconds: Int? = null,
    ) {
        mutableConnectionState.value = ChatConnectionState(
            accountId = accountId,
            status = status,
            retryDelaySeconds = retryDelaySeconds,
            errorCode = errorCode,
        )
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
        messageOrder = messageOrder,
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

    private class ChatStreamException(val failure: MobileClientFailure) : Exception()

    private companion object {
        const val PAGE_SIZE = 100
        const val MAX_SYNC_PAGES = 100
        val UNCERTAIN_DELIVERY_ERRORS = setOf(
            "network_unavailable",
            "backend_unavailable",
            "chat_unavailable",
            "service_unavailable",
        )
        val TERMINAL_SESSION_ERRORS = setOf(
            "invalid_token",
            "token_expired",
            "device_revoked",
            "account_disabled",
        )
    }
}
