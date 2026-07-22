package top.etta.aerie.di

import android.content.Context
import top.etta.aerie.data.session.InMemorySessionRepository
import top.etta.aerie.data.session.SessionRepository

class AppContainer(context: Context) {
    val applicationContext: Context = context.applicationContext

    val sessionRepository: SessionRepository = InMemorySessionRepository()
}
