package top.etta.aerie.data.session

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InMemorySessionRepositoryTest {
    private lateinit var repository: InMemorySessionRepository

    @Before
    fun setUp() {
        repository = InMemorySessionRepository()
    }

    @Test
    fun `login rejects short username before contacting backend`() = runTest {
        val result = repository.login(validInput().copy(username = "ab"))

        assertTrue(result is LoginResult.Failure)
        assertEquals(SessionState.SignedOut, repository.session.value)
    }

    @Test
    fun `login rejects short password before contacting backend`() = runTest {
        val result = repository.login(validInput().copy(password = "too-short"))

        assertTrue(result is LoginResult.Failure)
        assertEquals(SessionState.SignedOut, repository.session.value)
    }

    @Test
    fun `login requires an eight digit pairing code`() = runTest {
        val result = repository.login(validInput().copy(pairingCode = "1234abcd"))

        assertTrue(result is LoginResult.Failure)
        assertEquals(SessionState.SignedOut, repository.session.value)
    }

    @Test
    fun `valid contract remains signed out until server phase two exists`() = runTest {
        val result = repository.login(validInput())

        assertTrue(result is LoginResult.Failure)
        assertEquals(SessionState.SignedOut, repository.session.value)
    }

    @Test
    fun `debug preview preserves role and logout clears session`() {
        repository.enterLocalPreview(UserRole.Owner)

        val signedIn = repository.session.value as SessionState.SignedIn
        assertEquals(UserRole.Owner, signedIn.session.role)
        assertTrue(signedIn.session.isLocalPreview)

        repository.logout()

        assertEquals(SessionState.SignedOut, repository.session.value)
    }

    private fun validInput() = LoginInput(
        serverUrl = "https://aerie.etta.top",
        username = "owner",
        password = "correct-horse-battery",
        pairingCode = "12345678",
        deviceName = "VIVO Y500 Pro",
    )
}
