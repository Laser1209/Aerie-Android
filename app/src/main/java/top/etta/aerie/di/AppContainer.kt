package top.etta.aerie.di

import android.content.Context
import top.etta.aerie.BuildConfig
import top.etta.aerie.data.chat.AerieChatDatabase
import top.etta.aerie.data.chat.ChatRepository
import top.etta.aerie.data.chat.NetworkChatRepository
import top.etta.aerie.data.chat.RoomChatLocalStore
import top.etta.aerie.data.remote.AuthorizedRequestExecutor
import top.etta.aerie.data.remote.MobileApiErrorMapper
import top.etta.aerie.data.remote.MobileApiFactory
import top.etta.aerie.data.remote.ServerUrlPolicy
import top.etta.aerie.data.security.AndroidKeystoreSessionStore
import top.etta.aerie.data.session.NetworkSessionRepository
import top.etta.aerie.data.session.SessionRepository

class AppContainer(context: Context) {
    val applicationContext: Context = context.applicationContext

    private val apiFactory = MobileApiFactory()
    private val secureSessionStore = AndroidKeystoreSessionStore(applicationContext)
    val sessionRepository: SessionRepository = NetworkSessionRepository(
        apiFactory = apiFactory::create,
        secureStore = secureSessionStore,
        serverUrlPolicy = ServerUrlPolicy(allowLocalHttp = BuildConfig.DEBUG),
        errorMapper = MobileApiErrorMapper(),
    )
    private val chatDatabase = AerieChatDatabase.create(applicationContext)
    val chatRepository: ChatRepository = NetworkChatRepository(
        sessionRepository = sessionRepository,
        authorizedExecutor = AuthorizedRequestExecutor(sessionRepository),
        apiFactory = apiFactory::createChat,
        localStore = RoomChatLocalStore(chatDatabase.chatDao()),
        errorMapper = MobileApiErrorMapper(),
    )
}
