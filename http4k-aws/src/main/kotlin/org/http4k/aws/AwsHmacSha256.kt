package org.http4k.aws

import org.http4k.util.Hex.hex
import java.security.MessageDigest.getInstance
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object AwsHmacSha256 {

    fun hash(payload: String): String = hash(payload.toByteArray())

    fun hash(payload: ByteArray): String = hex(getInstance("SHA-256").digest(payload))

    fun hmacSHA256(key: ByteArray, data: String): ByteArray {
        val algorithm = "HmacSHA256"
        return Mac.getInstance(algorithm).run {
            init(SecretKeySpec(key, algorithm))
            doFinal(data.toByteArray(charset("UTF8")))
        }
    }
}
