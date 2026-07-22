package top.etta.aerie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import top.etta.aerie.ui.AerieApp
import top.etta.aerie.ui.AerieViewModel
import top.etta.aerie.ui.AerieViewModelFactory
import top.etta.aerie.ui.theme.AerieTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
}
