package top.etta.aerie.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class StoredSession(
    val serverUrl: String,
    val refreshToken: String,
    val accountId: String,
    val deviceId: String,
    val username: String,
    val role: String,
)

interface SecureSessionStore {
    suspend fun read(): StoredSession?
    suspend fun write(session: StoredSession)
    suspend fun clear()
}

private val Context.secureSessionDataStore by preferencesDataStore(
    name = "secure_mobile_session",
)

class AndroidKeystoreSessionStore(
    context: Context,
) : SecureSessionStore {
    private val dataStore = context.applicationContext.secureSessionDataStore

    override suspend fun read(): StoredSession? = withContext(Dispatchers.IO) {
        val preferences = dataStore.data.first()
        val encrypted = preferences[Keys.refreshToken] ?: return@withContext null
        val refreshToken = runCatching { decrypt(encrypted) }.getOrElse {
            deleteKey()
            clear()
            return@withContext null
        }
        val serverUrl = preferences[Keys.serverUrl] ?: return@withContext null
        val accountId = preferences[Keys.accountId] ?: return@withContext null
        val deviceId = preferences[Keys.deviceId] ?: return@withContext null
        val username = preferences[Keys.username] ?: return@withContext null
        val role = preferences[Keys.role] ?: return@withContext null
        StoredSession(serverUrl, refreshToken, accountId, deviceId, username, role)
    }

    override suspend fun write(session: StoredSession) {
        withContext(Dispatchers.IO) {
            val encryptedRefreshToken = encrypt(session.refreshToken)
            dataStore.edit { preferences ->
                preferences[Keys.refreshToken] = encryptedRefreshToken
                preferences[Keys.serverUrl] = session.serverUrl
                preferences[Keys.accountId] = session.accountId
                preferences[Keys.deviceId] = session.deviceId
                preferences[Keys.username] = session.username
                preferences[Keys.role] = session.role
            }
        }
    }

    override suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val ciphertext = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return listOf(cipher.iv, ciphertext).joinToString(".") {
            Base64.encodeToString(it, Base64.NO_WRAP)
        }
    }

    private fun decrypt(encoded: String): String {
        val parts = encoded.split('.', limit = 2)
        require(parts.size == 2) { "invalid encrypted session" }
        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val ciphertext = Base64.decode(parts[1], Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
            generateKey()
        }
    }

    private fun deleteKey() {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
            deleteEntry(KEY_ALIAS)
        }
    }

    private object Keys {
        val refreshToken = stringPreferencesKey("refresh_token_ciphertext")
        val serverUrl = stringPreferencesKey("server_url")
        val accountId = stringPreferencesKey("account_id")
        val deviceId = stringPreferencesKey("device_id")
        val username = stringPreferencesKey("username")
        val role = stringPreferencesKey("role")
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "aerie_mobile_refresh_token_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
