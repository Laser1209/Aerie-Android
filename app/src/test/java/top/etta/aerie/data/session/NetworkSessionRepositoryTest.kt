package top.etta.aerie.data.session

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import top.etta.aerie.data.remote.MobileApiFactory
import top.etta.aerie.data.remote.AuthorizedRequestExecutor
import top.etta.aerie.data.remote.ServerUrlPolicy
import top.etta.aerie.data.security.SecureSessionStore
import top.etta.aerie.data.security.StoredSession
import retrofit2.HttpException
import retrofit2.Response

class NetworkSessionRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var secureStore: FakeSecureSessionStore

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        secureStore = FakeSecureSessionStore()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `login persists only refresh session and exposes access token in memory`() = runTest {
        server.enqueue(jsonResponse(tokenResponse("access-one", "refresh-one")))
        val repository = repository()

        val result = repository.login(validInput())

        assertEquals(LoginResult.Success, result)
        assertEquals("access-one", repository.accessToken.value)
        assertEquals("refresh-one", secureStore.value?.refreshToken)
        val signedIn = repository.session.value as SessionState.SignedIn
        assertEquals(UserRole.Owner, signedIn.session.role)
        assertEquals("acct_owner", signedIn.session.accountId)
        val request = server.takeRequest()
        assertEquals("/api/mobile/v1/auth/login", request.path)
        assertTrue(request.body.readUtf8().contains("\"pairingCode\":\"12345678\""))
    }

    @Test
    fun `concurrent stale refresh calls rotate token only once`() = runTest {
        val refreshCount = AtomicInteger()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                "/api/mobile/v1/auth/login" -> jsonResponse(tokenResponse("access-one", "refresh-one"))
                "/api/mobile/v1/auth/refresh" -> {
                    refreshCount.incrementAndGet()
                    jsonResponse(tokenResponse("access-two", "refresh-two"))
                }
                else -> MockResponse().setResponseCode(404)
            }
        }
        val repository = repository()
        assertEquals(LoginResult.Success, repository.login(validInput()))

        val results = coroutineScope {
            List(12) {
                async { repository.refreshAccessToken(staleAccessToken = "access-one") }
            }.awaitAll()
        }

        assertEquals(1, refreshCount.get())
        assertTrue(results.all { it == "access-two" })
        assertEquals("refresh-two", secureStore.value?.refreshToken)
    }

    @Test
    fun `non stale refresh reuses the current access token without rotating`() = runTest {
        server.enqueue(jsonResponse(tokenResponse("access-one", "refresh-one")))
        val repository = repository()
        assertEquals(LoginResult.Success, repository.login(validInput()))

        val results = coroutineScope {
            List(12) { async { repository.refreshAccessToken() } }.awaitAll()
        }

        assertTrue(results.all { it == "access-one" })
        assertEquals("refresh-one", secureStore.value?.refreshToken)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `stable backend error is mapped without exposing server detail`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"error":{"code":"invalid_credentials","message":"internal detail","requestId":"req_1"}}""",
                ),
        )

        val result = repository().login(validInput()) as LoginResult.Failure

        assertEquals("invalid_credentials", result.code)
        assertEquals("用户名、密码或配对码无效", result.message)
        assertNull(secureStore.value)
    }

    @Test
    fun `authorized executor refreshes once and retries a 401`() = runTest {
        server.enqueue(jsonResponse(tokenResponse("access-one", "refresh-one")))
        server.enqueue(jsonResponse(tokenResponse("access-two", "refresh-two")))
        val repository = repository()
        repository.login(validInput())
        val executor = AuthorizedRequestExecutor(repository)
        val attempts = AtomicInteger()

        val result = executor.execute { token ->
            if (attempts.getAndIncrement() == 0) {
                throw HttpException(
                    Response.error<String>(
                        401,
                        "{}".toResponseBody("application/json".toMediaType()),
                    ),
                )
            }
            token
        }

        assertEquals("access-two", result)
        assertEquals(2, attempts.get())
        assertEquals("/api/mobile/v1/auth/refresh", server.takeRequest().let {
            if (it.path == "/api/mobile/v1/auth/login") server.takeRequest().path else it.path
        })
    }

    @Test
    fun `logout clears memory and encrypted session even when backend is unavailable`() = runTest {
        server.enqueue(jsonResponse(tokenResponse("access-one", "refresh-one")))
        val repository = repository()
        repository.login(validInput())
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        repository.logout()

        assertEquals(SessionState.SignedOut, repository.session.value)
        assertNull(repository.accessToken.value)
        assertNull(secureStore.value)
    }

    @Test
    fun `server URL policy rejects cleartext remote hosts`() {
        val policy = ServerUrlPolicy(allowLocalHttp = true)

        val error = runCatching { policy.normalize("http://192.168.1.50:7891") }.exceptionOrNull()
        val queryError = runCatching { policy.normalize("https://aerie.etta.top/?x=1") }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertTrue(queryError is IllegalArgumentException)
        assertEquals("https://aerie.etta.top/", policy.normalize("https://aerie.etta.top/path"))
    }

    private fun repository(): NetworkSessionRepository {
        val factory = MobileApiFactory()
        return NetworkSessionRepository(
            apiFactory = factory::create,
            secureStore = secureStore,
            serverUrlPolicy = ServerUrlPolicy(allowLocalHttp = true),
        )
    }

    private fun validInput() = LoginInput(
        serverUrl = server.url("/").toString(),
        username = "owner",
        password = "correct-horse-battery",
        pairingCode = "12345678",
        deviceName = "VIVO Y500 Pro",
    )

    private fun tokenResponse(accessToken: String, refreshToken: String): String =
        """{
          "accessToken":"$accessToken",
          "refreshToken":"$refreshToken",
          "accessExpiresIn":900,
          "refreshExpiresIn":2592000,
          "account":{
            "accountId":"acct_owner",
            "username":"owner",
            "role":"owner",
            "actorId":"actor_primary",
            "userId":"3489352115",
            "deviceId":"device_v2516a"
          }
        }""".trimIndent()

    private fun jsonResponse(body: String) = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(body)

    private class FakeSecureSessionStore : SecureSessionStore {
        var value: StoredSession? = null

        override suspend fun read(): StoredSession? = value

        override suspend fun write(session: StoredSession) {
            value = session
        }

        override suspend fun clear() {
            value = null
        }
    }
}
