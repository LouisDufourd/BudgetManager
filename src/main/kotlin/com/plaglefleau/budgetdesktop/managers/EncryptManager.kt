package com.plaglefleau.budgetdesktop.managers

import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class EncryptManager {
    companion object {
        /**
         * Creates a SecretKey from the provided key string.
         * The key string is hashed to the required length for AES.
         *
         * @param key The key string to be used.
         * @param keySize The desired AES key size in bytes (16, 24, or 32 bytes).
         * @return The SecretKeySpec for AES encryption.
         */
        fun getKeyFromString(key: String, keySize: Int): SecretKey {
            if (keySize !in arrayOf(16, 24, 32)) {
                throw IllegalArgumentException("Invalid AES key size. Must be 16, 24, or 32 bytes.")
            }

            val keyBytes = hashToBytes(key, keySize)
            return SecretKeySpec(keyBytes, "AES")
        }

        /**
         * Computes the HMAC-SHA-256 hash of the input string using the provided key.
         *
         * @param key The secret key for HMAC.
         * @param input The string to hash.
         * @return The HMAC-SHA-256 hash of the input string, represented as a Base64 encoded string.
         */
        fun hmacSha256(key: String, input: String): String {
            val mac = Mac.getInstance("HmacSHA256")
            val secretKey = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256")
            mac.init(secretKey)
            val hashBytes = mac.doFinal(input.toByteArray(Charsets.UTF_8))
            return Base64.getEncoder().encodeToString(hashBytes)
        }

        /**
         * Encrypts the provided data using AES encryption with the given key.
         *
         * @param data The data to be encrypted.
         * @param key The SecretKey to be used for encryption.
         * @return The encrypted data as a string.
         */
        fun encrypt(data: String, key: SecretKey): String {
            val cipher = Cipher.getInstance("AES")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            return Base64.getEncoder().encodeToString(encryptedBytes)
        }

        /**
         * Decrypts the provided data using AES encryption with the given key.
         *
         * @param encryptedData The encrypted data to be decrypted.
         * @param key The secret key for decryption.
         * @return The decrypted data as a string. If decryption fails, returns "0.0".
         */
        fun decrypt(encryptedData: String, key: SecretKey): String {
            val cipher = Cipher.getInstance("AES")
            cipher.init(Cipher.DECRYPT_MODE, key)
            try {
                val decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData))
                return String(decryptedBytes, Charsets.UTF_8)
            } catch (e: Exception) {
                return "0.0"
            }
        }

        /**
         * Computes a fixed-length hash of the input string.
         *
         * This function uses SHA-256 to hash the input string and then truncates the result to the specified length.
         *
         * @param input The string to hash.
         * @param length The desired length of the output hash in bytes (e.g., 16, 24, or 32).
         * @return A byte array of the specified length representing the hash.
         */
        private fun hashToBytes(input: String, length: Int): ByteArray {
            require(length in arrayOf(16, 24, 32)) { "Invalid length. Must be 16, 24, or 32 bytes." }

            // Create SHA-256 digest instance
            val digest = MessageDigest.getInstance("SHA-256")

            // Compute the hash
            val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))

            // Return the specified length of the hash
            return hashBytes.copyOf(length)
        }
    }
}