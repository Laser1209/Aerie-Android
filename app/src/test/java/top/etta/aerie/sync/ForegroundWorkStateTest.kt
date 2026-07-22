package top.etta.aerie.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import top.etta.aerie.data.chat.ChatRequestRecord
import top.etta.aerie.data.chat.PendingOutbound
import top.etta.aerie.data.chat.PendingOutboundState

class ForegroundWorkStateTest {
    @Test
    fun `only active request statuses keep the service active`() {
        val state = foregroundWorkState(
            requests = listOf(
                request("queued"),
                request("running"),
                request("cancel_requested"),
                request("completed"),
                request("failed"),
            ),
            pendingOutbound = emptyList(),
        )

        assertTrue(state.isActive)
        assertEquals(3, state.activeRequestCount)
        assertEquals(ForegroundWorkKind.Executing, state.kind)
    }

    @Test
    fun `awaiting confirmation is not treated as background work`() {
        val state = foregroundWorkState(
            requests = emptyList(),
            pendingOutbound = listOf(
                pending(PendingOutboundState.AwaitingConfirmation),
            ),
        )

        assertFalse(state.isActive)
        assertEquals(null, state.kind)
    }

    @Test
    fun `an in-flight send uses the transfer notification state`() {
        val state = foregroundWorkState(
            requests = emptyList(),
            pendingOutbound = listOf(pending(PendingOutboundState.Sending)),
        )

        assertTrue(state.isActive)
        assertEquals(ForegroundWorkKind.Transferring, state.kind)
    }

    @Test
    fun `approval has priority over transfer and execution`() {
        val state = ForegroundWorkState(
            activeRequestCount = 1,
            activeTransferCount = 1,
            pendingApprovalCount = 1,
        )

        assertEquals(ForegroundWorkKind.AwaitingApproval, state.kind)
    }

    private fun request(status: String) = ChatRequestRecord(
        accountId = "acct-owner",
        requestId = "req-$status",
        conversationId = "conv-owner",
        clientRequestId = "client-$status",
        status = status,
        errorCode = null,
        retryOfRequestId = null,
        createdAt = "2026-07-22T00:00:00Z",
        updatedAt = null,
        completedAt = null,
    )

    private fun pending(state: PendingOutboundState) = PendingOutbound(
        accountId = "acct-owner",
        clientRequestId = "client-${state.storedValue}",
        text = "not included in a notification",
        state = state,
        createdAt = "2026-07-22T00:00:00Z",
    )
}
