package org.http4k.aws

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object AwsHmacSha256 {

    fun hash(payload: String): String = hash(payload.toByteArray())

    fun hash(payload: ByteArray): String {
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val res = digest.digest(payload)
            return hex(res)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        }
    }

    fun hmacSHA256(key: ByteArray, data: String): ByteArray {
        try {
            val algorithm = "HmacSHA256"
            val mac = Mac.getInstance(algorithm)
            mac.init(SecretKeySpec(key, algorithm))
            return mac.doFinal(data.toByteArray(charset("UTF8")))
        } catch (e: Exception) {
            throw RuntimeException("Could not run HMAC SHA256", e)
        }
    }

    fun hex(data: ByteArray): String {
        val result = StringBuilder()
        for (aByte in data) {
            result.append(String.format("%02x", aByte))
        }
        return result.toString().toLowerCase()
    }
}
