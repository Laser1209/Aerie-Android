package top.etta.aerie.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import top.etta.aerie.data.chat.ChatOperationResult
import top.etta.aerie.data.session.ActiveSession
import top.etta.aerie.data.session.UserRole

class PeriodicSyncTest {
    @Test
    fun `missing stored session completes without synchronizing`() = runTest {
        var synchronizeCalls = 0

        val outcome = runPeriodicSync(
            restoreSession = { false },
            currentSession = { null },
            synchronize = {
                synchronizeCalls += 1
                ChatOperationResult.Success()
            },
        )

        assertEquals(PeriodicSyncOutcome.Completed, outcome)
        assertEquals(0, synchronizeCalls)
    }

    @Test
    fun `restored remote session synchronizes its account`() = runTest {
        val accounts = mutableListOf<String>()

        val outcome = runPeriodicSync(
            restoreSession = { true },
            currentSession = { remoteSession() },
            synchronize = { accountId ->
                accounts += accountId
                ChatOperationResult.Success()
            },
        )

        assertEquals(PeriodicSyncOutcome.Completed, outcome)
        assertEquals(listOf("acct-owner"), accounts)
    }

    @Test
    fun `local preview never performs background network work`() = runTest {
        var synchronizeCalls = 0

        val outcome = runPeriodicSync(
            restoreSession = { true },
            currentSession = { remoteSession().copy(isLocalPreview = true) },
            synchronize = {
                synchronizeCalls += 1
                ChatOperationResult.Success()
            },
        )

        assertEquals(PeriodicSyncOutcome.Completed, outcome)
        assertEquals(0, synchronizeCalls)
    }

    @Test
    fun `recoverable synchronization failure requests retry`() = runTest {
        val outcome = runPeriodicSync(
            restoreSession = { true },
            currentSession = { remoteSession() },
            synchronize = {
                ChatOperationResult.Failure("backend_unavailable", "服务器暂时不可用")
            },
        )

        assertEquals(PeriodicSyncOutcome.Retry, outcome)
    }

    @Test
    fun `cancellation is never converted into retry`() {
        assertThrows(CancellationException::class.java) {
            runTest {
                runPeriodicSync(
                    restoreSession = { true },
                    currentSession = { remoteSession() },
                    synchronize = { throw CancellationException("cancelled") },
                )
            }
        }
    }

    private fun remoteSession() = ActiveSession(
        accountId = "acct-owner",
        deviceId = "device-owner",
        displayName = "owner",
        role = UserRole.Owner,
        isLocalPreview = false,
    )
}
