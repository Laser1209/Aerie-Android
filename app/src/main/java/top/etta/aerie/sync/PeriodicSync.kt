package top.etta.aerie.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import top.etta.aerie.AerieApplication
import top.etta.aerie.data.chat.ChatOperationResult
import top.etta.aerie.data.session.ActiveSession
import top.etta.aerie.data.session.SessionState

enum class PeriodicSyncOutcome {
    Completed,
    Retry,
}

internal suspend fun runPeriodicSync(
    restoreSession: suspend () -> Boolean,
    currentSession: () -> ActiveSession?,
    synchronize: suspend (String) -> ChatOperationResult,
): PeriodicSyncOutcome = try {
    if (!restoreSession()) {
        PeriodicSyncOutcome.Completed
    } else {
        val session = currentSession()?.takeUnless { it.isLocalPreview }
        if (session == null) {
            PeriodicSyncOutcome.Completed
        } else {
            when (synchronize(session.accountId)) {
                is ChatOperationResult.Failure -> PeriodicSyncOutcome.Retry
                is ChatOperationResult.AwaitingConfirmation,
                is ChatOperationResult.Success,
                -> PeriodicSyncOutcome.Completed
            }
        }
    }
} catch (error: CancellationException) {
    throw error
} catch (_: Throwable) {
    PeriodicSyncOutcome.Retry
}

interface PeriodicSyncScheduler {
    fun ensureScheduled()
    fun cancel()
}

object NoOpPeriodicSyncScheduler : PeriodicSyncScheduler {
    override fun ensureScheduled() = Unit
    override fun cancel() = Unit
}

class AndroidPeriodicSyncScheduler(context: Context) : PeriodicSyncScheduler {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    override fun ensureScheduled() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<AeriePeriodicSyncWorker>(
            PERIOD_MINUTES,
            TimeUnit.MINUTES,
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                RETRY_BACKOFF_MINUTES,
                TimeUnit.MINUTES,
            )
            .addTag(WORK_TAG)
            .build()
        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    override fun cancel() {
        workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    companion object {
        const val UNIQUE_WORK_NAME = "aerie_periodic_status_sync"
        const val WORK_TAG = "aerie_background_sync"
        const val PERIOD_MINUTES = 15L
        const val RETRY_BACKOFF_MINUTES = 5L
    }
}

class AeriePeriodicSyncWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val application = applicationContext as? AerieApplication
            ?: return Result.failure()
        val container = application.appContainer
        val outcome = runPeriodicSync(
            restoreSession = container.sessionRepository::restoreSession,
            currentSession = {
                (container.sessionRepository.session.value as? SessionState.SignedIn)?.session
            },
            synchronize = container.chatRepository::synchronize,
        )
        return when (outcome) {
            PeriodicSyncOutcome.Completed -> Result.success()
            PeriodicSyncOutcome.Retry -> Result.retry()
        }
    }
}
