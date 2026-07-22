package top.etta.aerie.data.remote

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MobileEventStreamTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `connect sends authorization and last event id and ignores heartbeat comments`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(
                    "event: stream.open\n" +
                        "data: {}\n\n" +
                        ": heartbeat\n\n" +
                        "id: evt_7\n" +
                        "event: message.created\n" +
                        "data: {\"messageId\":\"msg_7\"}\n\n",
                ),
        )
        val frames = mutableListOf<MobileSseFrame>()

        val result = MobileEventStream(
            server.url("/").toString(),
            OkHttpClient(),
        ).connect(
            accessToken = "access-test",
            lastEventId = "evt_6",
            onFrame = { frames += it },
        )

        assertTrue(result is MobileEventStreamResult.Closed)
        assertEquals(
            listOf("stream.open", "message.created"),
            frames.map { it.type },
        )
        assertEquals("evt_7", frames.last().id)
        val request = checkNotNull(server.takeRequest(5, TimeUnit.SECONDS))
        assertEquals("Bearer access-test", request.getHeader("Authorization"))
        assertEquals("evt_6", request.getHeader("Last-Event-ID"))
        assertEquals("text/event-stream", request.getHeader("Accept"))
    }

    @Test
    fun `http unauthorized is returned without exposing response content`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("secret server detail"))

        val result = MobileEventStream(
            server.url("/").toString(),
            OkHttpClient(),
        ).connect(accessToken = "expired-access", lastEventId = null)

        assertTrue(result is MobileEventStreamResult.Failed)
        assertEquals(401, (result as MobileEventStreamResult.Failed).statusCode)
        assertTrue((result.cause?.message ?: "").contains("secret server detail").not())
    }
}
