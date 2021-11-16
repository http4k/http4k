package org.http4k.security

import org.http4k.util.Hex
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object HmacSha256 {

    fun hash(payload: String): String = hash(payload.toByteArray())

    fun hash(payload: ByteArray): String = Hex.hex(MessageDigest.getInstance("SHA-256").digest(payload))

    fun hmacSHA256(key: ByteArray, data: String): ByteArray = Mac.getInstance("HmacSHA256").run {
        init(SecretKeySpec(key, "HmacSHA256"))
        doFinal(data.toByteArray(charset("UTF8")))
    }
}
