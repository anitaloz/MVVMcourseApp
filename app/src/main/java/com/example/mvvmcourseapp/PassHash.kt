package com.example.mvvmcourseapp

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PassHash {
    private const val SALT_LENGTH = 32
    private const val ITERATIONS = 10000
    private const val KEY_LENGTH = 256

    // Генерация соли
    private fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        random.nextBytes(salt)
        return salt
    }

    // Хэширование пароля с солью
    fun hashPassword(password: String): String {
        val salt = generateSalt()
        val spec = PBEKeySpec(
            password.toCharArray(),
            salt,
            ITERATIONS,
            KEY_LENGTH
        )
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return "${ITERATIONS}:${bytesToHex(salt)}:${bytesToHex(hash)}"
    }

    // Проверка пароля
    fun verifyPassword(password: String, storedPassword: String): Boolean {
        val parts = storedPassword.split(":")
        if (parts.size != 3) return false

        val iterations = parts[0].toInt()
        val salt = hexToBytes(parts[1])
        val hash = hexToBytes(parts[2])

        val spec = PBEKeySpec(
            password.toCharArray(),
            salt,
            iterations,
            hash.size * 8
        )
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val testHash = factory.generateSecret(spec).encoded

        return MessageDigest.isEqual(hash, testHash)
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hexToBytes(hex: String): ByteArray {
        return hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}