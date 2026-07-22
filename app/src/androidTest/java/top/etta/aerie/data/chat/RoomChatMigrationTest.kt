package top.etta.aerie.data.chat

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomChatMigrationTest {
    @Test
    fun migrationToV2PreservesMessagesAndForcesResync() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val databaseName = "migration-${UUID.randomUUID()}.db"
        val databaseFile = context.getDatabasePath(databaseName)
        createV1Database(databaseFile)

        val database = Room.databaseBuilder(
            context,
            AerieChatDatabase::class.java,
            databaseName,
        ).addMigrations(MIGRATION_1_2).allowMainThreadQueries().build()
        try {
            val store = RoomChatLocalStore(database.chatDao())
            assertEquals(
                listOf("msg-first", "msg-second"),
                store.observeMessages("acct_owner").first().map { it.messageId },
            )
            assertEquals(1L, store.observeMessages("acct_owner").first()[0].messageOrder)
            assertEquals(2L, store.observeMessages("acct_owner").first()[1].messageOrder)
            assertNull(store.getCursor("acct_owner"))
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
    }

    private fun createV1Database(file: File) {
        file.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { database ->
            database.execSQL(
                """CREATE TABLE chat_messages (
                    accountId TEXT NOT NULL,
                    messageId TEXT NOT NULL,
                    conversationId TEXT NOT NULL,
                    turnId TEXT,
                    role TEXT NOT NULL,
                    content TEXT NOT NULL,
                    attachmentsJson TEXT NOT NULL,
                    createdAt TEXT NOT NULL,
                    PRIMARY KEY(accountId, messageId)
                )""",
            )
            database.execSQL(
                """CREATE INDEX index_chat_messages_accountId_createdAt
                   ON chat_messages(accountId, createdAt)""",
            )
            database.execSQL(
                """CREATE TABLE chat_requests (
                    accountId TEXT NOT NULL,
                    requestId TEXT NOT NULL,
                    conversationId TEXT,
                    clientRequestId TEXT,
                    status TEXT NOT NULL,
                    errorCode TEXT,
                    retryOfRequestId TEXT,
                    createdAt TEXT,
                    updatedAt TEXT,
                    completedAt TEXT,
                    PRIMARY KEY(accountId, requestId)
                )""",
            )
            database.execSQL(
                """CREATE INDEX index_chat_requests_accountId_status
                   ON chat_requests(accountId, status)""",
            )
            database.execSQL(
                """CREATE TABLE pending_outbound (
                    accountId TEXT NOT NULL,
                    clientRequestId TEXT NOT NULL,
                    text TEXT NOT NULL,
                    state TEXT NOT NULL,
                    createdAt TEXT NOT NULL,
                    lastErrorCode TEXT,
                    PRIMARY KEY(accountId, clientRequestId)
                )""",
            )
            database.execSQL(
                """CREATE INDEX index_pending_outbound_accountId_state
                   ON pending_outbound(accountId, state)""",
            )
            database.execSQL(
                """CREATE TABLE chat_sync_cursors (
                    accountId TEXT NOT NULL PRIMARY KEY,
                    latestMessageId TEXT,
                    lastEventId TEXT,
                    syncedAt TEXT NOT NULL
                )""",
            )
            database.execSQL(
                """CREATE TABLE room_master_table (
                    id INTEGER PRIMARY KEY,
                    identity_hash TEXT
                )""",
            )
            database.execSQL(
                """INSERT INTO room_master_table(id, identity_hash)
                   VALUES(42, '92e9fafbba6b66ba0009c242e64075ec')""",
            )
            database.execSQL(
                """INSERT INTO chat_messages
                   (accountId, messageId, conversationId, turnId, role, content,
                    attachmentsJson, createdAt)
                   VALUES ('acct_owner', 'msg-first', 'conv_owner', NULL, 'user',
                           'question', '[]', '2026-07-22T13:00:28Z')""",
            )
            database.execSQL(
                """INSERT INTO chat_messages
                   (accountId, messageId, conversationId, turnId, role, content,
                    attachmentsJson, createdAt)
                   VALUES ('acct_owner', 'msg-second', 'conv_owner', NULL, 'assistant',
                           'answer', '[]', '2026-07-22T13:00:28Z')""",
            )
            database.execSQL(
                """INSERT INTO chat_sync_cursors
                   (accountId, latestMessageId, lastEventId, syncedAt)
                   VALUES ('acct_owner', 'msg-second', 'evt_9', '2026-07-22T13:00:28Z')""",
            )
            database.version = 1
        }
    }
}
