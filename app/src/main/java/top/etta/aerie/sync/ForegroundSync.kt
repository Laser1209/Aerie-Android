package top.etta.aerie.sync

import android.Manifest
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
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import top.etta.aerie.MainActivity
import top.etta.aerie.R
import top.etta.aerie.data.chat.ChatRequestRecord
import top.etta.aerie.data.chat.PendingOutbound
import top.etta.aerie.data.chat.PendingOutboundState

enum class ForegroundWorkKind {
    Executing,
    Transferring,
    AwaitingApproval,
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
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onTimeout(startId: Int, fgsType: Int) {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf(startId)
    }

    private fun startWithNotification(kind: ForegroundWorkKind) {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openApp = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val statusText = when (kind) {
            ForegroundWorkKind.Executing -> R.string.foreground_status_executing
            ForegroundWorkKind.Transferring -> R.string.foreground_status_transferring
            ForegroundWorkKind.AwaitingApproval -> R.string.foreground_status_awaiting_approval
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_aerie)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(statusText))
            .setContentIntent(openApp)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setSilent(true)
            .build()
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
        private const val CHANNEL_ID = "aerie_data_sync"
        private const val NOTIFICATION_ID = 4101
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
