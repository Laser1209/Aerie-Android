package top.etta.aerie

import android.app.Application
import top.etta.aerie.di.AppContainer

class AerieApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(applicationContext)
    }
}
