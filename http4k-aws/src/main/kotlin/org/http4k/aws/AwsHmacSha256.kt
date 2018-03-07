package org.http4k.aws

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object AwsHmacSha256 {

    fun hash(payload: String): String = hash(payload.toByteArray())

    fun hash(payload: ByteArray): String = try {
        hex(MessageDigest.getInstance("SHA-256").digest(payload))
    } catch (e: NoSuchAlgorithmException) {
        throw RuntimeException(e)
    }

    fun hmacSHA256(key: ByteArray, data: String): ByteArray = try {
        val algorithm = "HmacSHA256"
        Mac.getInstance(algorithm).run {
            init(SecretKeySpec(key, algorithm))
            doFinal(data.toByteArray(charset("UTF8")))
        }
    } catch (e: Exception) {
        throw RuntimeException("Could not run HMAC SHA256", e)
    }

    fun hex(data: ByteArray): String = data.fold(StringBuilder()) { acc, next ->
        acc.append(String.format("%02x", next))
    }.toString().toLowerCase()
}
