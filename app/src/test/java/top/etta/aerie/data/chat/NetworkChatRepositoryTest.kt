package top.etta.aerie.data.chat

import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import top.etta.aerie.data.remote.AuthorizedRequestExecutor
import top.etta.aerie.data.remote.MobileApiFactory
import top.etta.aerie.data.remote.MobileChatApi
import top.etta.aerie.data.remote.MobileEventStream
import top.etta.aerie.data.remote.MobileMessagePageDto
import top.etta.aerie.data.remote.MobileRequestDto
import top.etta.aerie.data.remote.SubmitMobileRequestDto
import top.etta.aerie.data.session.ActiveSession
import top.etta.aerie.data.session.LoginInput
import top.etta.aerie.data.session.LoginResult
import top.etta.aerie.data.session.SessionRepository
import top.etta.aerie.data.session.SessionState
import top.etta.aerie.data.session.UserRole

class NetworkChatRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var session: FakeSessionRepository
    private lateinit var local: FakeChatLocalStore

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        session = FakeSessionRepository(server.url("/").toString())
        local = FakeChatLocalStore()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `synchronize loads complete history and converges active request`() = runTest {
        val observedPaths = CopyOnWriteArrayList<String>()
        val observedAuth = CopyOnWriteArrayList<String?>()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                observedPaths += request.path.orEmpty()
                observedAuth += request.getHeader("Authorization")
                return when {
                    request.path?.startsWith("/api/mobile/v1/messages?") == true &&
                        request.requestUrl?.queryParameter("beforeId") == null -> jsonResponse(
                        messagePage(
                            items = listOf(message("msg_2", "second"), message("msg_3", "third")),
                            hasMore = true,
                        ),
                    )
                    request.requestUrl?.queryParameter("beforeId") == "msg_2" -> jsonResponse(
                        messagePage(listOf(message("msg_1", "first")), hasMore = false),
                    )
                    request.path == "/api/mobile/v1/requests/req_active" -> jsonResponse(
                        requestJson("req_active", "completed"),
                    )
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        local.upsertRequest(
            ChatRequestRecord(
                accountId = ACCOUNT_ID,
                requestId = "req_active",
                conversationId = "conv_1",
                clientRequestId = "client_1",
                status = "running",
                errorCode = null,
                retryOfRequestId = null,
                createdAt = "2026-07-22T00:00:00Z",
                updatedAt = null,
                completedAt = null,
            ),
        )

        val result = repository().synchronize(ACCOUNT_ID)

        assertTrue(result is ChatOperationResult.Success)
        assertEquals(
            listOf("msg_1", "msg_2", "msg_3"),
            local.observeMessages(ACCOUNT_ID).first().map { it.messageId },
        )
        assertEquals("msg_3", local.getCursor(ACCOUNT_ID)?.latestMessageId)
        assertEquals("completed", local.observeRequests(ACCOUNT_ID).first().single().status)
        assertTrue(observedPaths.any { "beforeId=msg_2" in it })
        assertTrue(observedAuth.all { it == "Bearer access-one" })
    }

    @Test
    fun `active request refresh converges status without loading messages`() = runTest {
        server.enqueue(jsonResponse(requestJson("req_active", "completed")))
        local.upsertRequest(
            ChatRequestRecord(
                accountId = ACCOUNT_ID,
                requestId = "req_active",
                conversationId = "conv_1",
                clientRequestId = "client_1",
                status = "running",
                errorCode = null,
                retryOfRequestId = null,
                createdAt = "2026-07-22T00:00:00Z",
                updatedAt = null,
                completedAt = null,
            ),
        )

        val result = repository().refreshActiveRequests(ACCOUNT_ID)

        assertTrue(result is ChatOperationResult.Success)
        assertEquals("completed", local.observeRequests(ACCOUNT_ID).first().single().status)
        assertEquals("/api/mobile/v1/requests/req_active", server.takeRequest().path)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `synchronize preserves server order when message timestamps match`() = runTest {
        server.enqueue(
            jsonResponse(
                messagePage(
                    items = listOf(
                        message("msg-z-user", "question", messageOrder = 100),
                        message("msg-a-answer-1", "answer 1", messageOrder = 101),
                        message("msg-y-answer-2", "answer 2", messageOrder = 102),
                        message("msg-b-answer-3", "answer 3", messageOrder = 103),
                    ),
                    hasMore = false,
                ),
            ),
        )

        val result = repository().synchronize(ACCOUNT_ID)

        assertTrue(result is ChatOperationResult.Success)
        assertEquals(
            listOf("msg-z-user", "msg-a-answer-1", "msg-y-answer-2", "msg-b-answer-3"),
            local.observeMessages(ACCOUNT_ID).first().map { it.messageId },
        )
    }

    @Test
    fun `uncertain submit waits for confirmation and reuses client request id`() = runTest {
        val submittedIds = mutableListOf<String>()
        var attempt = 0
        val api = object : StubMobileChatApi() {
            override suspend fun submitRequest(
                authorization: String,
                body: SubmitMobileRequestDto,
            ): MobileRequestDto {
                submittedIds += body.clientRequestId
                attempt += 1
                if (attempt == 1) throw IOException("connection lost after send")
                return MobileRequestDto(
                    requestId = "req_fixed",
                    conversationId = "conv_1",
                    status = "queued",
                    clientRequestId = body.clientRequestId,
                )
            }
        }
        val repository = repository(api = api)

        val first = repository.submit(ACCOUNT_ID, "hello once")
        val pending = local.observePending(ACCOUNT_ID).first().single()
        val confirmed = repository.confirmPending(ACCOUNT_ID, pending.clientRequestId)

        assertTrue(first is ChatOperationResult.AwaitingConfirmation)
        assertTrue(confirmed is ChatOperationResult.Success)
        assertEquals(listOf(FIXED_CLIENT_ID, FIXED_CLIENT_ID), submittedIds)
        assertTrue(local.observePending(ACCOUNT_ID).first().isEmpty())
        assertEquals(
            FIXED_CLIENT_ID,
            local.observeRequests(ACCOUNT_ID).first().single().clientRequestId,
        )
    }

    @Test
    fun `cancel and retry persist server request states`() = runTest {
        server.enqueue(jsonResponse(requestJson("req_1", "cancel_requested")))
        server.enqueue(
            jsonResponse(
                """{"requestId":"req_2","conversationId":"conv_1","status":"queued","retryOfRequestId":"req_1"}""",
            ),
        )
        val repository = repository()

        val cancelled = repository.cancel(ACCOUNT_ID, "req_1")
        val retried = repository.retry(ACCOUNT_ID, "req_1")

        assertTrue(cancelled is ChatOperationResult.Success)
        assertTrue(retried is ChatOperationResult.Success)
        val rows = local.observeRequests(ACCOUNT_ID).first().associateBy { it.requestId }
        assertEquals("cancel_requested", rows.getValue("req_1").status)
        assertEquals("req_1", rows.getValue("req_2").retryOfRequestId)
        assertEquals("/api/mobile/v1/requests/req_1/cancel", server.takeRequest().path)
        assertEquals("/api/mobile/v1/requests/req_1/retry", server.takeRequest().path)
    }

    @Test
    fun `repository rejects a different account before network access`() = runTest {
        val result = repository().synchronize("acct_other")

        assertEquals("invalid_token", (result as ChatOperationResult.Failure).code)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `event stream reconnects with persisted cursor and applies message`() = runTest {
        server.enqueue(
            sseResponse(
                "id: evt_1\n" +
                    "event: message.created\n" +
                    "data: {\"messageId\":\"msg_1\",\"messageOrder\":1,\"conversationId\":\"conv_1\",\"role\":\"assistant\",\"content\":\"hello\",\"createdAt\":\"2026-07-22T00:00:00Z\"}\n\n",
            ),
        )
        server.enqueue(
            sseResponse(
                "id: evt_2\n" +
                    "event: request.updated\n" +
                    "data: {\"requestId\":\"req_1\",\"conversationId\":\"conv_1\",\"status\":\"completed\",\"updatedAt\":\"2026-07-22T00:01:00Z\"}\n\n",
            ),
        )
        val repository = repositoryWithEventStream()
        val job = launch {
            repository.runEventStream(ACCOUNT_ID)
        }

        val message = local.observeMessages(ACCOUNT_ID).first { it.isNotEmpty() }
        assertEquals("msg_1", message.single().messageId)
        local.observeRequests(ACCOUNT_ID).first { rows ->
            rows.any { it.requestId == "req_1" }
        }
        job.cancel()
        job.join()

        val firstRequest = checkNotNull(server.takeRequest(5, TimeUnit.SECONDS))
        val secondRequest = checkNotNull(server.takeRequest(5, TimeUnit.SECONDS))
        assertTrue(firstRequest.path?.endsWith("/api/mobile/v1/events") == true)
        assertEquals("Bearer access-one", firstRequest.getHeader("Authorization"))
        assertEquals(null, firstRequest.getHeader("Last-Event-ID"))
        assertTrue(secondRequest.path?.endsWith("/api/mobile/v1/events") == true)
        assertEquals("evt_1", secondRequest.getHeader("Last-Event-ID"))
        assertEquals("evt_2", local.getCursor(ACCOUNT_ID)?.lastEventId)
    }

    private fun repository(api: MobileChatApi? = null): NetworkChatRepository {
        val apiFactory = MobileApiFactory()
        return NetworkChatRepository(
            sessionRepository = session,
            authorizedExecutor = AuthorizedRequestExecutor(session),
            apiFactory = { api ?: apiFactory.createChat(it) },
            localStore = local,
            newClientRequestId = { FIXED_CLIENT_ID },
            now = { "2026-07-22T00:00:00Z" },
        )
    }

    private fun repositoryWithEventStream(): NetworkChatRepository {
        val api = object : StubMobileChatApi() {
            override suspend fun messages(
                authorization: String,
                beforeId: String?,
                afterId: String?,
                limit: Int,
            ): MobileMessagePageDto = MobileMessagePageDto(emptyList(), hasMore = false)
        }
        return NetworkChatRepository(
            sessionRepository = session,
            authorizedExecutor = AuthorizedRequestExecutor(session),
            apiFactory = { api },
            localStore = local,
            eventStreamFactory = { url -> MobileEventStream(url, OkHttpClient()) },
            sleep = {},
            random = { 0.5 },
        )
    }

    private fun message(
        id: String,
        content: String,
        messageOrder: Long = id.removePrefix("msg_").toLongOrNull() ?: 1L,
    ): String =
        """{"messageId":"$id","messageOrder":$messageOrder,"conversationId":"conv_1","turnId":"turn_1","role":"user","content":"$content","attachments":[],"createdAt":"2026-07-22T00:00:00Z"}"""

    private fun messagePage(items: List<String>, hasMore: Boolean): String =
        """{"items":[${items.joinToString(",")}],"hasMore":$hasMore}"""

    private fun requestJson(id: String, status: String): String =
        """{"requestId":"$id","conversationId":"conv_1","status":"$status","createdAt":"2026-07-22T00:00:00Z","updatedAt":"2026-07-22T00:01:00Z"}"""

    private fun jsonResponse(body: String) = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(body)

    private fun sseResponse(body: String) = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "text/event-stream")
        .setBody(body)

    private open class StubMobileChatApi : MobileChatApi {
        override suspend fun messages(
            authorization: String,
            beforeId: String?,
            afterId: String?,
            limit: Int,
        ): MobileMessagePageDto = error("unexpected messages call")

        override suspend fun submitRequest(
            authorization: String,
            body: SubmitMobileRequestDto,
        ): MobileRequestDto = error("unexpected submit call")

        override suspend fun request(
            authorization: String,
            requestId: String,
        ): MobileRequestDto = error("unexpected request call")

        override suspend fun cancelRequest(
            authorization: String,
            requestId: String,
        ): MobileRequestDto = error("unexpected cancel call")

        override suspend fun retryRequest(
            authorization: String,
            requestId: String,
        ): MobileRequestDto = error("unexpected retry call")
    }

    private class FakeSessionRepository(server: String) : SessionRepository {
        private val mutableSession = MutableStateFlow<SessionState>(
            SessionState.SignedIn(
                ActiveSession(ACCOUNT_ID, "device_1", "owner", UserRole.Owner, false),
            ),
        )
        override val session = mutableSession.asStateFlow()
        private val mutableAccessToken = MutableStateFlow<String?>("access-one")
        override val accessToken = mutableAccessToken.asStateFlow()
        private val mutableServerUrl = MutableStateFlow<String?>(server)
        override val serverUrl = mutableServerUrl.asStateFlow()

        override suspend fun login(input: LoginInput): LoginResult = LoginResult.Success
        override suspend fun restoreSession(): Boolean = true
        override suspend fun refreshAccessToken(staleAccessToken: String?): String? =
            mutableAccessToken.value
        override fun enterLocalPreview(role: UserRole) = Unit
        override suspend fun logout() = Unit
    }

    private class FakeChatLocalStore : ChatLocalStore {
        private val messages = MutableStateFlow<List<ChatMessage>>(emptyList())
        private val requests = MutableStateFlow<List<ChatRequestRecord>>(emptyList())
        private val pending = MutableStateFlow<List<PendingOutbound>>(emptyList())
        private val cursors = mutableMapOf<String, ChatSyncCursor>()

        override fun observeMessages(accountId: String): Flow<List<ChatMessage>> =
            messages.map { rows ->
                rows.filter { it.accountId == accountId }.sortedBy { it.messageOrder }
            }

        override fun observeRequests(accountId: String): Flow<List<ChatRequestRecord>> =
            requests.map { rows -> rows.filter { it.accountId == accountId } }

        override fun observePending(accountId: String): Flow<List<PendingOutbound>> =
            pending.map { rows -> rows.filter { it.accountId == accountId } }

        override suspend fun upsertMessages(messages: List<ChatMessage>) {
            val keys = messages.map { it.accountId to it.messageId }.toSet()
            this.messages.value = this.messages.value.filterNot {
                (it.accountId to it.messageId) in keys
            } + messages
        }

        override suspend fun upsertRequest(request: ChatRequestRecord) {
            val old = requests.value.firstOrNull {
                it.accountId == request.accountId && it.requestId == request.requestId
            }
            val merged = request.copy(
                conversationId = request.conversationId ?: old?.conversationId,
                clientRequestId = request.clientRequestId ?: old?.clientRequestId,
                createdAt = request.createdAt ?: old?.createdAt,
            )
            requests.value = requests.value.filterNot {
                it.accountId == request.accountId && it.requestId == request.requestId
            } + merged
        }

        override suspend fun activeRequests(accountId: String): List<ChatRequestRecord> =
            requests.value.filter {
                it.accountId == accountId && it.status in setOf("queued", "running", "cancel_requested")
            }

        override suspend fun getPending(
            accountId: String,
            clientRequestId: String,
        ): PendingOutbound? = pending.value.firstOrNull {
            it.accountId == accountId && it.clientRequestId == clientRequestId
        }

        override suspend fun upsertPending(pending: PendingOutbound) {
            this.pending.value = this.pending.value.filterNot {
                it.accountId == pending.accountId && it.clientRequestId == pending.clientRequestId
            } + pending
        }

        override suspend fun deletePending(accountId: String, clientRequestId: String) {
            pending.value = pending.value.filterNot {
                it.accountId == accountId && it.clientRequestId == clientRequestId
            }
        }

        override suspend fun recoverInterruptedSends(accountId: String) {
            pending.value = pending.value.map {
                if (it.accountId == accountId && it.state == PendingOutboundState.Sending) {
                    it.copy(state = PendingOutboundState.AwaitingConfirmation)
                } else {
                    it
                }
            }
        }

        override suspend fun getCursor(accountId: String): ChatSyncCursor? = cursors[accountId]
        override suspend fun upsertCursor(cursor: ChatSyncCursor) {
            cursors[cursor.accountId] = cursor
        }
        override suspend fun clearCursor(accountId: String) {
            cursors.remove(accountId)
        }
    }

    private companion object {
        const val ACCOUNT_ID = "acct_owner"
        const val FIXED_CLIENT_ID = "00000000-0000-4000-8000-000000000123"
    }
}
