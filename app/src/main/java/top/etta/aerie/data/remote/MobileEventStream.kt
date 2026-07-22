package top.etta.aerie.data.remote

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.channels.Channel
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources

data class MobileSseFrame(
    val id: String?,
    val type: String?,
    val data: String,
)

sealed interface MobileEventStreamResult {
    data object Closed : MobileEventStreamResult

    data class Failed(
        val statusCode: Int?,
        val cause: Throwable?,
    ) : MobileEventStreamResult
}

/** A single authenticated SSE connection. Reconnect policy belongs to the repository. */
class MobileEventStream(
    private val baseUrl: String,
    private val eventSourceFactory: EventSource.Factory,
) {
    constructor(
        baseUrl: String,
        client: OkHttpClient,
    ) : this(
        baseUrl = baseUrl,
        eventSourceFactory = EventSources.createFactory(
            client.newBuilder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build(),
        ),
    )

    suspend fun connect(
        accessToken: String,
        lastEventId: String?,
        onOpen: suspend () -> Unit = {},
        onFrame: suspend (MobileSseFrame) -> Unit = {},
    ): MobileEventStreamResult {
        require(accessToken.isNotBlank()) { "access token must not be blank" }
        val request = Request.Builder()
            .url(eventUrl())
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")
            .header("Authorization", "Bearer $accessToken")
            .apply {
                if (!lastEventId.isNullOrBlank()) {
                    header("Last-Event-ID", lastEventId)
                }
            }
            .build()
        val signals = Channel<Signal>(Channel.UNLIMITED)
        val listener = object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                signals.trySend(Signal.Open)
            }

            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String,
            ) {
                signals.trySend(Signal.Frame(MobileSseFrame(id, type, data)))
            }

            override fun onClosed(eventSource: EventSource) {
                signals.trySend(Signal.Closed)
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?,
            ) {
                signals.trySend(
                    Signal.Failure(
                        statusCode = response?.code,
                        cause = t ?: IOException("SSE connection failed"),
                    ),
                )
            }
        }
        val eventSource = eventSourceFactory.newEventSource(request, listener)
        return try {
            for (signal in signals) {
                when (signal) {
                    Signal.Open -> onOpen()
                    is Signal.Frame -> onFrame(signal.frame)
                    Signal.Closed -> return MobileEventStreamResult.Closed
                    is Signal.Failure -> return MobileEventStreamResult.Failed(
                        statusCode = signal.statusCode,
                        cause = signal.cause,
                    )
                }
            }
            MobileEventStreamResult.Closed
        } finally {
            eventSource.cancel()
            signals.close()
        }
    }

    private fun eventUrl() = baseUrl.toHttpUrlOrNull()
        ?.newBuilder()
        ?.addPathSegments("api/mobile/v1/events")
        ?.build()
        ?: throw IllegalArgumentException("server URL is invalid")

    private sealed interface Signal {
        data object Open : Signal
        data class Frame(val frame: MobileSseFrame) : Signal
        data object Closed : Signal
        data class Failure(val statusCode: Int?, val cause: Throwable?) : Signal
    }
}
