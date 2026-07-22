package top.etta.aerie

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import top.etta.aerie.data.session.SessionState
import top.etta.aerie.ui.AerieApp
import top.etta.aerie.ui.AerieViewModel
import top.etta.aerie.ui.AerieViewModelFactory
import top.etta.aerie.ui.theme.AerieTheme

class MainActivity : ComponentActivity() {
    private var notificationPermissionRequested = false
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        (application as AerieApplication).appContainer.foregroundSyncController
            .refreshCapability()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeNotificationPermissionNeed()
        enableEdgeToEdge()
        setContent {
            AerieTheme {
                val application = application as AerieApplication
                val viewModel: AerieViewModel = viewModel(
                    factory = AerieViewModelFactory(application.appContainer),
                )
                AerieApp(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (application as AerieApplication).appContainer.foregroundSyncController
            .refreshCapability()
    }

    private fun observeNotificationPermissionNeed() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                (application as AerieApplication).appContainer.sessionRepository.session
                    .map { state ->
                        (state as? SessionState.SignedIn)?.session
                            ?.takeUnless { it.isLocalPreview } != null
                    }
                    .distinctUntilChanged()
                    .collect { hasRemoteSession ->
                        if (hasRemoteSession) requestNotificationPermissionIfNeeded()
                    }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotificationPermissionIfNeeded() {
        if (notificationPermissionRequested ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notificationPermissionRequested = true
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
