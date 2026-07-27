package com.onehouse.app.feature.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class HomeAssistantSettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val tokenCipher = TokenCipher()

    fun read(): HomeAssistantSettings {
        val baseUrl = preferences.getString(KEY_BASE_URL, "").orEmpty()
        val token = preferences.getString(KEY_TOKEN, null)
            ?.let(tokenCipher::decrypt)
            .orEmpty()
        val storedStatus = preferences.getString(KEY_LAST_STATUS, null)
            ?.let { runCatching { HomeAssistantConnectionStatus.valueOf(it) }.getOrNull() }
            ?.takeUnless { it == HomeAssistantConnectionStatus.TESTING }
            ?: if (baseUrl.isBlank() || token.isBlank()) {
                HomeAssistantConnectionStatus.NOT_CONFIGURED
            } else {
                HomeAssistantConnectionStatus.NOT_TESTED
            }

        return HomeAssistantSettings(
            baseUrl = baseUrl,
            accessToken = token,
            lastStatus = storedStatus,
            lastMessage = preferences.getString(KEY_LAST_MESSAGE, "Sin comprobar")
                .orEmpty()
                .ifBlank { "Sin comprobar" },
            lastTestEpochMillis = preferences.getLong(KEY_LAST_TEST, 0L)
        )
    }

    fun write(settings: HomeAssistantSettings) {
        preferences.edit()
            .putString(KEY_BASE_URL, settings.baseUrl.trim())
            .putString(KEY_TOKEN, tokenCipher.encrypt(settings.accessToken))
            .putString(KEY_LAST_STATUS, settings.lastStatus.name)
            .putString(KEY_LAST_MESSAGE, settings.lastMessage)
            .putLong(KEY_LAST_TEST, settings.lastTestEpochMillis)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "onehouse_home_assistant"
        const val KEY_BASE_URL = "base_url"
        const val KEY_TOKEN = "access_token"
        const val KEY_LAST_STATUS = "last_status"
        const val KEY_LAST_MESSAGE = "last_message"
        const val KEY_LAST_TEST = "last_test"
    }
}

private class TokenCipher {
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }

    fun encrypt(value: String): String {
        if (value.isBlank()) return ""
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
            val payload = cipher.iv + encrypted
            Base64.encodeToString(payload, Base64.NO_WRAP)
        }.getOrDefault("")
    }

    fun decrypt(value: String): String {
        if (value.isBlank()) return ""
        return runCatching {
            val payload = Base64.decode(value, Base64.NO_WRAP)
            require(payload.size > IV_SIZE)
            val iv = payload.copyOfRange(0, IV_SIZE)
            val encrypted = payload.copyOfRange(IV_SIZE, payload.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_SIZE_BITS, iv))
            String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
        }.getOrDefault("")
    }

    private fun getOrCreateKey(): SecretKey {
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "onehouse_home_assistant_token"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val TAG_SIZE_BITS = 128
    }
}
