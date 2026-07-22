package top.etta.aerie.sync

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import top.etta.aerie.AerieApplication
import top.etta.aerie.MainActivity
import top.etta.aerie.R
import top.etta.aerie.data.chat.ChatOperationResult
import top.etta.aerie.data.chat.ChatRepository
import top.etta.aerie.data.chat.ChatRequestRecord
import top.etta.aerie.data.chat.PendingOutbound
import top.etta.aerie.data.chat.PendingOutboundState
import top.etta.aerie.data.session.SessionState

private const val CHANNEL_ID = "aerie_data_sync"
private const val NOTIFICATION_ID = 4101
private const val TAG = "AerieForegroundSync"
internal const val FOREGROUND_SERVICE_BEHAVIOR = NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE

enum class ForegroundWorkKind {
    Executing,
    Transferring,
    AwaitingApproval,
}

internal fun buildForegroundNotification(
    context: Context,
    kind: ForegroundWorkKind,
): Notification {
    val openAppIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val openApp = PendingIntent.getActivity(
        context,
        0,
        openAppIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val statusText = when (kind) {
        ForegroundWorkKind.Executing -> R.string.foreground_status_executing
        ForegroundWorkKind.Transferring -> R.string.foreground_status_transferring
        ForegroundWorkKind.AwaitingApproval -> R.string.foreground_status_awaiting_approval
    }
    return NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_stat_aerie)
        .setContentTitle(context.getString(R.string.app_name))
        .setContentText(context.getString(statusText))
        .setContentIntent(openApp)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOnlyAlertOnce(true)
        .setOngoing(true)
        .setSilent(true)
        .setForegroundServiceBehavior(FOREGROUND_SERVICE_BEHAVIOR)
        .build()
}

data class ForegroundWorkState(
    val activeRequestCount: Int = 0,
    val activeTransferCount: Int = 0,
    val pendingApprovalCount: Int = 0,
) {
    val isActive: Boolean
        get() = activeRequestCount > 0 || activeTransferCount > 0 || pendingApprovalCount > 0

    val kind: ForegroundWorkKind?
        get() = when {
            pendingApprovalCount > 0 -> ForegroundWorkKind.AwaitingApproval
            activeTransferCount > 0 -> ForegroundWorkKind.Transferring
            activeRequestCount > 0 -> ForegroundWorkKind.Executing
            else -> null
        }
}

fun foregroundWorkState(
    requests: List<ChatRequestRecord>,
    pendingOutbound: List<PendingOutbound>,
): ForegroundWorkState = ForegroundWorkState(
    activeRequestCount = requests.count { it.status in ACTIVE_REQUEST_STATUSES },
    activeTransferCount = pendingOutbound.count { it.state == PendingOutboundState.Sending },
)

internal enum class ForegroundExecutionPollDecision {
    Continue,
    Stop,
}

internal suspend fun pollForegroundExecution(
    readState: suspend () -> ForegroundWorkState,
    refreshActiveRequests: suspend () -> ChatOperationResult,
): ForegroundExecutionPollDecision {
    val beforeSync = readState()
    if (!beforeSync.isActive) return ForegroundExecutionPollDecision.Stop
    if (beforeSync.activeRequestCount > 0) {
        refreshActiveRequests()
    }

    return if (readState().isActive) {
        ForegroundExecutionPollDecision.Continue
    } else {
        ForegroundExecutionPollDecision.Stop
    }
}

enum class ForegroundSyncCapability {
    Available,
    NotificationsUnavailable,
    StartRestricted,
}

interface ForegroundSyncController {
    val capability: StateFlow<ForegroundSyncCapability>

    fun update(state: ForegroundWorkState)
    fun refreshCapability()
    fun stop()
}

object NoOpForegroundSyncController : ForegroundSyncController {
    private val available = MutableStateFlow(ForegroundSyncCapability.Available)
    override val capability: StateFlow<ForegroundSyncCapability> = available.asStateFlow()

    override fun update(state: ForegroundWorkState) = Unit
    override fun refreshCapability() = Unit
    override fun stop() = Unit
}

class AndroidForegroundSyncController(context: Context) : ForegroundSyncController {
    private val applicationContext = context.applicationContext
    private val mutableCapability = MutableStateFlow(readCapability())
    override val capability: StateFlow<ForegroundSyncCapability> = mutableCapability.asStateFlow()
    private var requestedState = ForegroundWorkState()

    override fun update(state: ForegroundWorkState) {
        requestedState = state
        applyRequestedState()
    }

    override fun refreshCapability() {
        mutableCapability.value = readCapability()
        applyRequestedState()
    }

    override fun stop() {
        requestedState = ForegroundWorkState()
        applicationContext.stopService(AerieForegroundService.stopIntent(applicationContext))
    }

    private fun applyRequestedState() {
        if (!requestedState.isActive) {
            applicationContext.stopService(AerieForegroundService.stopIntent(applicationContext))
            return
        }
        val permissionState = readCapability()
        mutableCapability.value = permissionState
        if (permissionState != ForegroundSyncCapability.Available) {
            applicationContext.stopService(AerieForegroundService.stopIntent(applicationContext))
            return
        }
        val kind = checkNotNull(requestedState.kind)
        try {
            ContextCompat.startForegroundService(
                applicationContext,
                AerieForegroundService.startIntent(applicationContext, kind),
            )
        } catch (_: IllegalStateException) {
            mutableCapability.value = ForegroundSyncCapability.StartRestricted
        } catch (_: SecurityException) {
            mutableCapability.value = ForegroundSyncCapability.StartRestricted
        }
    }

    private fun readCapability(): ForegroundSyncCapability {
        val runtimePermissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        return if (runtimePermissionGranted &&
            NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()
        ) {
            ForegroundSyncCapability.Available
        } else {
            ForegroundSyncCapability.NotificationsUnavailable
        }
    }
}

class AerieForegroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var executionMonitor: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_START) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf(startId)
            return START_NOT_STICKY
        }
        val kind = intent.getStringExtra(EXTRA_KIND)
            ?.let { stored -> ForegroundWorkKind.entries.firstOrNull { it.name == stored } }
            ?: ForegroundWorkKind.Executing
        startWithNotification(kind)
        ensureExecutionMonitor()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        executionMonitor?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onTimeout(startId: Int, fgsType: Int) {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf(startId)
    }

    private fun startWithNotification(kind: ForegroundWorkKind) {
        val notification = buildForegroundNotification(this, kind)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun ensureExecutionMonitor() {
        if (executionMonitor?.isActive == true) return
        executionMonitor = serviceScope.launch {
            Log.i(TAG, "foreground work monitor started")
            monitorExecutionUntilTerminal()
        }
    }

    private suspend fun monitorExecutionUntilTerminal() {
        val container = (application as? AerieApplication)?.appContainer
            ?: return stopAfterExecution()
        var session = (container.sessionRepository.session.value as? SessionState.SignedIn)?.session
        if (session == null && container.sessionRepository.restoreSession()) {
            session = (container.sessionRepository.session.value as? SessionState.SignedIn)?.session
        }
        val activeSession = session?.takeUnless { it.isLocalPreview }
            ?: return stopAfterExecution()

        while (currentCoroutineContext().isActive) {
            val decision = try {
                withTimeoutOrNull(EXECUTION_SYNC_TIMEOUT_MILLIS) {
                    pollForegroundExecution(
                        readState = {
                            readForegroundWorkState(container.chatRepository, activeSession.accountId)
                        },
                        refreshActiveRequests = {
                            container.chatRepository.refreshActiveRequests(activeSession.accountId)
                        },
                    )
                } ?: run {
                    Log.w(TAG, "foreground request refresh timed out")
                    ForegroundExecutionPollDecision.Continue
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Log.w(TAG, "foreground request refresh failed: ${error::class.java.simpleName}")
                ForegroundExecutionPollDecision.Continue
            }
            when (decision) {
                ForegroundExecutionPollDecision.Stop -> {
                    Log.i(TAG, "foreground work reached terminal state")
                    return stopAfterExecution()
                }
                ForegroundExecutionPollDecision.Continue -> delay(EXECUTION_POLL_INTERVAL_MILLIS)
            }
        }
    }

    private suspend fun readForegroundWorkState(
        repository: ChatRepository,
        accountId: String,
    ): ForegroundWorkState = combine(
        repository.observeRequests(accountId),
        repository.observePending(accountId),
        ::foregroundWorkState,
    ).first()

    private fun stopAfterExecution() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.foreground_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.foreground_channel_description)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        internal const val EXECUTION_POLL_INTERVAL_MILLIS = 5_000L
        internal const val EXECUTION_SYNC_TIMEOUT_MILLIS = 20_000L
        private const val ACTION_START = "top.etta.aerie.action.START_DATA_SYNC"
        private const val ACTION_STOP = "top.etta.aerie.action.STOP_DATA_SYNC"
        private const val EXTRA_KIND = "foreground_work_kind"

        fun startIntent(context: Context, kind: ForegroundWorkKind): Intent =
            Intent(context, AerieForegroundService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_KIND, kind.name)

        fun stopIntent(context: Context): Intent =
            Intent(context, AerieForegroundService::class.java).setAction(ACTION_STOP)
    }
}

private val ACTIVE_REQUEST_STATUSES = setOf("queued", "running", "cancel_requested")
