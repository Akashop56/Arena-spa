package com.ronin.ai.core.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts secrets (API keys) with AES-256-GCM where the master key never
 * leaves the Android Keystore hardware/trusted environment.
 *
 * Format: base64(iv):base64(ciphertext+tag)
 */
class SecureVault {

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun masterKey(): SecretKey {
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    fun encrypt(plain: String): String {
        return try {
            doEncrypt(plain)
        } catch (t: Throwable) {
            // The Keystore entry can be invalidated by the system (e.g. secure
            // lock-screen changes). Regenerate once rather than failing the save.
            runCatching { keyStore.deleteEntry(KEY_ALIAS) }
            doEncrypt(plain)
        }
    }

    private fun doEncrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, masterKey())
        val ciphertext = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv
        return Base64.encodeToString(iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(ciphertext, Base64.NO_WRAP)
    }

    /** Returns null when the blob cannot be decrypted (e.g. key invalidated). */
    fun decrypt(blob: String): String? = runCatching {
        val separator = blob.indexOf(':')
        if (separator <= 0) return null
        val iv = Base64.decode(blob.substring(0, separator), Base64.NO_WRAP)
        val ciphertext = Base64.decode(blob.substring(separator + 1), Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, masterKey(), GCMParameterSpec(128, iv))
        String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }.getOrNull()

    fun hasKey(): Boolean = keyStore.containsAlias(KEY_ALIAS)

    fun deleteKey() {
        keyStore.deleteEntry(KEY_ALIAS)
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "ronin_master_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
