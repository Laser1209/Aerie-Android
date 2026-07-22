package top.etta.aerie.data.chat

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.ColumnInfo
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(
    tableName = "chat_messages",
    primaryKeys = ["accountId", "messageId"],
    indices = [Index(value = ["accountId", "messageOrder"])],
)
data class ChatMessageEntity(
    val accountId: String,
    val messageId: String,
    @ColumnInfo(defaultValue = "0")
    val messageOrder: Long,
    val conversationId: String,
    val turnId: String?,
    val role: String,
    val content: String,
    val attachmentsJson: String,
    val createdAt: String,
)

@Entity(
    tableName = "chat_requests",
    primaryKeys = ["accountId", "requestId"],
    indices = [Index(value = ["accountId", "status"])],
)
data class ChatRequestEntity(
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

@Entity(
    tableName = "pending_outbound",
    primaryKeys = ["accountId", "clientRequestId"],
    indices = [Index(value = ["accountId", "state"])],
)
data class PendingOutboundEntity(
    val accountId: String,
    val clientRequestId: String,
    val text: String,
    val state: String,
    val createdAt: String,
    val lastErrorCode: String?,
)

@Entity(tableName = "chat_sync_cursors")
data class ChatSyncCursorEntity(
    @androidx.room.PrimaryKey val accountId: String,
    val latestMessageId: String?,
    val lastEventId: String?,
    val syncedAt: String,
)

@Dao
interface ChatDao {
    @Query(
        """SELECT * FROM chat_messages WHERE accountId = :accountId
           ORDER BY messageOrder ASC, createdAt ASC, messageId ASC""",
    )
    fun observeMessages(accountId: String): Flow<List<ChatMessageEntity>>

    @Query(
        """SELECT * FROM chat_requests WHERE accountId = :accountId
           ORDER BY COALESCE(updatedAt, createdAt, '') DESC, requestId DESC""",
    )
    fun observeRequests(accountId: String): Flow<List<ChatRequestEntity>>

    @Query(
        """SELECT * FROM pending_outbound WHERE accountId = :accountId
           ORDER BY createdAt ASC, clientRequestId ASC""",
    )
    fun observePending(accountId: String): Flow<List<PendingOutboundEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessages(messages: List<ChatMessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRequest(request: ChatRequestEntity)

    @Query(
        """SELECT * FROM chat_requests
           WHERE accountId = :accountId AND status IN (:statuses)""",
    )
    suspend fun activeRequests(
        accountId: String,
        statuses: List<String>,
    ): List<ChatRequestEntity>

    @Query(
        """SELECT * FROM chat_requests
           WHERE accountId = :accountId AND requestId = :requestId LIMIT 1""",
    )
    suspend fun request(accountId: String, requestId: String): ChatRequestEntity?

    @Query(
        """SELECT * FROM pending_outbound
           WHERE accountId = :accountId AND clientRequestId = :clientRequestId LIMIT 1""",
    )
    suspend fun pending(
        accountId: String,
        clientRequestId: String,
    ): PendingOutboundEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPending(pending: PendingOutboundEntity)

    @Query(
        """DELETE FROM pending_outbound
           WHERE accountId = :accountId AND clientRequestId = :clientRequestId""",
    )
    suspend fun deletePending(accountId: String, clientRequestId: String)

    @Query(
        """UPDATE pending_outbound SET state = 'awaiting_confirmation'
           WHERE accountId = :accountId AND state = 'sending'""",
    )
    suspend fun recoverInterruptedSends(accountId: String)

    @Query("SELECT * FROM chat_sync_cursors WHERE accountId = :accountId LIMIT 1")
    suspend fun cursor(accountId: String): ChatSyncCursorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCursor(cursor: ChatSyncCursorEntity)

    @Query("DELETE FROM chat_sync_cursors WHERE accountId = :accountId")
    suspend fun clearCursor(accountId: String)
}

@Database(
    entities = [
        ChatMessageEntity::class,
        ChatRequestEntity::class,
        PendingOutboundEntity::class,
        ChatSyncCursorEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AerieChatDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao

    companion object {
        fun create(context: Context): AerieChatDatabase = Room.databaseBuilder(
            context.applicationContext,
            AerieChatDatabase::class.java,
            "aerie_mobile_chat.db",
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE chat_messages ADD COLUMN messageOrder INTEGER NOT NULL DEFAULT 0",
        )
        database.execSQL(
            "UPDATE chat_messages SET messageOrder = rowid WHERE messageOrder = 0",
        )
        database.execSQL("DROP INDEX IF EXISTS index_chat_messages_accountId_createdAt")
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_chat_messages_accountId_messageOrder " +
                "ON chat_messages (accountId, messageOrder)",
        )
        database.execSQL("DELETE FROM chat_sync_cursors")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DELETE FROM chat_sync_cursors")
    }
}

class RoomChatLocalStore(
    private val dao: ChatDao,
) : ChatLocalStore {
    override fun observeMessages(accountId: String): Flow<List<ChatMessage>> =
        dao.observeMessages(accountId).map { rows -> rows.map(ChatMessageEntity::toDomain) }

    override fun observeRequests(accountId: String): Flow<List<ChatRequestRecord>> =
        dao.observeRequests(accountId).map { rows -> rows.map(ChatRequestEntity::toDomain) }

    override fun observePending(accountId: String): Flow<List<PendingOutbound>> =
        dao.observePending(accountId).map { rows -> rows.map(PendingOutboundEntity::toDomain) }

    override suspend fun upsertMessages(messages: List<ChatMessage>) {
        if (messages.isNotEmpty()) dao.upsertMessages(messages.map(ChatMessage::toEntity))
    }

    override suspend fun upsertRequest(request: ChatRequestRecord) {
        val existing = dao.request(request.accountId, request.requestId)
        dao.upsertRequest(
            request.copy(
                conversationId = request.conversationId ?: existing?.conversationId,
                clientRequestId = request.clientRequestId ?: existing?.clientRequestId,
                createdAt = request.createdAt ?: existing?.createdAt,
                updatedAt = request.updatedAt ?: existing?.updatedAt,
            ).toEntity(),
        )
    }

    override suspend fun activeRequests(accountId: String): List<ChatRequestRecord> =
        dao.activeRequests(accountId, ACTIVE_REQUEST_STATUSES).map(ChatRequestEntity::toDomain)

    override suspend fun getPending(
        accountId: String,
        clientRequestId: String,
    ): PendingOutbound? = dao.pending(accountId, clientRequestId)?.toDomain()

    override suspend fun upsertPending(pending: PendingOutbound) {
        dao.upsertPending(pending.toEntity())
    }

    override suspend fun deletePending(accountId: String, clientRequestId: String) {
        dao.deletePending(accountId, clientRequestId)
    }

    override suspend fun recoverInterruptedSends(accountId: String) {
        dao.recoverInterruptedSends(accountId)
    }

    override suspend fun getCursor(accountId: String): ChatSyncCursor? =
        dao.cursor(accountId)?.toDomain()

    override suspend fun upsertCursor(cursor: ChatSyncCursor) {
        dao.upsertCursor(cursor.toEntity())
    }

    override suspend fun clearCursor(accountId: String) {
        dao.clearCursor(accountId)
    }

    private companion object {
        val ACTIVE_REQUEST_STATUSES = listOf("queued", "running", "cancel_requested")
    }
}

private fun ChatMessageEntity.toDomain() = ChatMessage(
    accountId, messageId, messageOrder, conversationId, turnId, role, content, attachmentsJson,
    createdAt,
)

private fun ChatMessage.toEntity() = ChatMessageEntity(
    accountId, messageId, messageOrder, conversationId, turnId, role, content, attachmentsJson,
    createdAt,
)

private fun ChatRequestEntity.toDomain() = ChatRequestRecord(
    accountId,
    requestId,
    conversationId,
    clientRequestId,
    status,
    errorCode,
    retryOfRequestId,
    createdAt,
    updatedAt,
    completedAt,
)

private fun ChatRequestRecord.toEntity() = ChatRequestEntity(
    accountId,
    requestId,
    conversationId,
    clientRequestId,
    status,
    errorCode,
    retryOfRequestId,
    createdAt,
    updatedAt,
    completedAt,
)

private fun PendingOutboundEntity.toDomain() = PendingOutbound(
    accountId = accountId,
    clientRequestId = clientRequestId,
    text = text,
    state = PendingOutboundState.entries.firstOrNull { it.storedValue == state }
        ?: PendingOutboundState.AwaitingConfirmation,
    createdAt = createdAt,
    lastErrorCode = lastErrorCode,
)

private fun PendingOutbound.toEntity() = PendingOutboundEntity(
    accountId, clientRequestId, text, state.storedValue, createdAt, lastErrorCode,
)

private fun ChatSyncCursorEntity.toDomain() = ChatSyncCursor(
    accountId, latestMessageId, lastEventId, syncedAt,
)

private fun ChatSyncCursor.toEntity() = ChatSyncCursorEntity(
    accountId, latestMessageId, lastEventId, syncedAt,
)
