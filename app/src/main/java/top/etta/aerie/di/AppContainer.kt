package top.etta.aerie.di

import android.content.Context
import top.etta.aerie.BuildConfig
import top.etta.aerie.data.remote.MobileApiErrorMapper
import top.etta.aerie.data.remote.MobileApiFactory
import top.etta.aerie.data.remote.ServerUrlPolicy
import top.etta.aerie.data.security.AndroidKeystoreSessionStore
import top.etta.aerie.data.session.NetworkSessionRepository
import top.etta.aerie.data.session.SessionRepository

class AppContainer(context: Context) {
    val applicationContext: Context = context.applicationContext

    private val apiFactory = MobileApiFactory()
    val sessionRepository: SessionRepository = NetworkSessionRepository(
        apiFactory = apiFactory::create,
        secureStore = AndroidKeystoreSessionStore(applicationContext),
        serverUrlPolicy = ServerUrlPolicy(allowLocalHttp = BuildConfig.DEBUG),
        errorMapper = MobileApiErrorMapper(),
    )
}
