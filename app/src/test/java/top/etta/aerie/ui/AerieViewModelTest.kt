package top.etta.aerie.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import top.etta.aerie.data.chat.ChatConnectionState
import top.etta.aerie.data.chat.ChatMessage
import top.etta.aerie.data.chat.ChatOperationResult
import top.etta.aerie.data.chat.ChatRepository
import top.etta.aerie.data.chat.ChatRequestRecord
import top.etta.aerie.data.chat.PendingOutbound
import top.etta.aerie.data.session.ActiveSession
import top.etta.aerie.data.session.LoginInput
import top.etta.aerie.data.session.LoginResult
import top.etta.aerie.data.session.SessionRepository
import top.etta.aerie.data.session.SessionState
import top.etta.aerie.data.session.UserRole

@OptIn(ExperimentalCoroutinesApi::class)
class AerieViewModelTest {
    private lateinit var session: FakeSessionRepository
    private lateinit var chat: FakeChatRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        session = FakeSessionRepository()
        chat = FakeChatRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `send uses the active remote account and settles action state`() = runTest {
        val viewModel = AerieViewModel(session, chat)

        viewModel.sendMessage("  hello from ui  ")

        assertEquals(listOf("acct-owner" to "  hello from ui  "), chat.submissions)
        assertFalse(viewModel.chatActionState.value.isBusy)
        assertEquals(null, viewModel.chatActionState.value.errorMessage)
    }

    @Test
    fun `switching accounts replaces the observed message flow`() = runTest {
        val viewModel = AerieViewModel(session, chat)
        backgroundScope.launch {
            viewModel.chatMessages.collect { }
        }
        advanceUntilIdle()
        chat.messagesFor("acct-owner").value = listOf(message("acct-owner", "owner history"))
        advanceUntilIdle()
        assertEquals("owner history", viewModel.chatMessages.value.single().content)

        session.setSession(
            SessionState.SignedIn(
                ActiveSession("acct-guest", "device-guest", "guest", UserRole.Guest, false),
            ),
        )
        advanceUntilIdle()
        assertEquals(emptyList<ChatMessage>(), viewModel.chatMessages.value)
        chat.messagesFor("acct-guest").value = listOf(message("acct-guest", "guest history"))
        advanceUntilIdle()
        assertEquals("guest history", viewModel.chatMessages.value.single().content)
    }

    private fun message(accountId: String, content: String) = ChatMessage(
        accountId = accountId,
        messageId = "msg-$accountId",
        messageOrder = 1,
        conversationId = "conv-$accountId",
        turnId = null,
        role = "assistant",
        content = content,
        attachmentsJson = "[]",
        createdAt = "2026-07-22T00:00:00Z",
    )

    private class FakeSessionRepository : SessionRepository {
        private val mutableSession = MutableStateFlow<SessionState>(
            SessionState.SignedIn(
                ActiveSession("acct-owner", "device-owner", "owner", UserRole.Owner, false),
            ),
        )
        override val session = mutableSession.asStateFlow()
        override val accessToken = MutableStateFlow<String?>("access-test").asStateFlow()
        override val serverUrl = MutableStateFlow<String?>("https://example.test/").asStateFlow()

        fun setSession(value: SessionState) {
            mutableSession.value = value
        }

        override suspend fun login(input: LoginInput): LoginResult = LoginResult.Success
        override suspend fun restoreSession(): Boolean = true
        override suspend fun refreshAccessToken(staleAccessToken: String?): String? = "access-test"
        override fun enterLocalPreview(role: UserRole) = Unit
        override suspend fun logout() {
            mutableSession.value = SessionState.SignedOut
        }
    }

    private class FakeChatRepository : ChatRepository {
        val submissions = mutableListOf<Pair<String, String>>()
        private val messageFlows = mutableMapOf<String, MutableStateFlow<List<ChatMessage>>>()

        fun messagesFor(accountId: String): MutableStateFlow<List<ChatMessage>> =
            messageFlows.getOrPut(accountId) { MutableStateFlow(emptyList()) }

        override fun observeMessages(accountId: String): Flow<List<ChatMessage>> =
            messagesFor(accountId)

        override fun observeRequests(accountId: String): Flow<List<ChatRequestRecord>> =
            flowOf(emptyList())

        override fun observePending(accountId: String): Flow<List<PendingOutbound>> =
            flowOf(emptyList())

        override fun observeConnection(accountId: String): Flow<ChatConnectionState> =
            flowOf(ChatConnectionState(accountId = accountId))

        override suspend fun synchronize(accountId: String): ChatOperationResult =
            ChatOperationResult.Success()

        override suspend fun runEventStream(accountId: String) = Unit

        override suspend fun submit(accountId: String, text: String): ChatOperationResult {
            submissions += accountId to text
            return ChatOperationResult.Success()
        }

        override suspend fun confirmPending(
            accountId: String,
            clientRequestId: String,
        ): ChatOperationResult = ChatOperationResult.Success()

        override suspend fun cancel(accountId: String, requestId: String): ChatOperationResult =
            ChatOperationResult.Success()

        override suspend fun retry(accountId: String, requestId: String): ChatOperationResult =
            ChatOperationResult.Success()
    }
}
