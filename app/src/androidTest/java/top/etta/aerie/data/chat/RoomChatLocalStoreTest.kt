package top.etta.aerie.data.chat

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomChatLocalStoreTest {
    private lateinit var database: AerieChatDatabase
    private lateinit var store: RoomChatLocalStore

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AerieChatDatabase::class.java,
        ).allowMainThreadQueries().build()
        store = RoomChatLocalStore(database.chatDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun messagesRequestsAndCursorsRemainAccountScoped() = runBlocking {
        store.upsertMessages(
            listOf(
                message("acct_owner", "msg_owner", "owner history"),
                message("acct_guest", "msg_guest", "guest history"),
            ),
        )
        store.upsertRequest(request("acct_owner", "req_owner", "running"))
        store.upsertRequest(request("acct_guest", "req_guest", "queued"))
        store.upsertCursor(cursor("acct_owner", "msg_owner", "evt_10"))
        store.upsertCursor(cursor("acct_guest", "msg_guest", "evt_20"))

        assertEquals(
            listOf("msg_owner"),
            store.observeMessages("acct_owner").first().map { it.messageId },
        )
        assertEquals(
            listOf("msg_guest"),
            store.observeMessages("acct_guest").first().map { it.messageId },
        )
        assertEquals(
            listOf("req_owner"),
            store.activeRequests("acct_owner").map { it.requestId },
        )
        assertEquals("evt_10", store.getCursor("acct_owner")?.lastEventId)
        assertEquals("evt_20", store.getCursor("acct_guest")?.lastEventId)

        store.clearCursor("acct_owner")
        assertNull(store.getCursor("acct_owner"))
        assertEquals("evt_20", store.getCursor("acct_guest")?.lastEventId)
    }

    @Test
    fun interruptedSendRequiresManualConfirmationAfterRestart() = runBlocking {
        store.upsertPending(pending("acct_owner", "client_owner"))
        store.upsertPending(pending("acct_guest", "client_guest"))

        store.recoverInterruptedSends("acct_owner")

        assertEquals(
            PendingOutboundState.AwaitingConfirmation,
            store.getPending("acct_owner", "client_owner")?.state,
        )
        assertEquals(
            PendingOutboundState.Sending,
            store.getPending("acct_guest", "client_guest")?.state,
        )
    }

    private fun message(accountId: String, messageId: String, content: String) = ChatMessage(
        accountId = accountId,
        messageId = messageId,
        conversationId = "conv_$accountId",
        turnId = null,
        role = "user",
        content = content,
        attachmentsJson = "[]",
        createdAt = "2026-07-22T00:00:00Z",
    )

    private fun request(accountId: String, requestId: String, status: String) =
        ChatRequestRecord(
            accountId = accountId,
            requestId = requestId,
            conversationId = "conv_$accountId",
            clientRequestId = "client_$requestId",
            status = status,
            errorCode = null,
            retryOfRequestId = null,
            createdAt = "2026-07-22T00:00:00Z",
            updatedAt = null,
            completedAt = null,
        )

    private fun cursor(accountId: String, messageId: String, eventId: String) = ChatSyncCursor(
        accountId = accountId,
        latestMessageId = messageId,
        lastEventId = eventId,
        syncedAt = "2026-07-22T00:00:00Z",
    )

    private fun pending(accountId: String, clientRequestId: String) = PendingOutbound(
        accountId = accountId,
        clientRequestId = clientRequestId,
        text = "send once",
        state = PendingOutboundState.Sending,
        createdAt = "2026-07-22T00:00:00Z",
    )
}
