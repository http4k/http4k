package org.http4k.security

object HmacSha256 {

    @Deprecated("Use Sha256.hash", ReplaceWith("Sha256.hash(payload)", "org.http4k.security.Sha256"))
    fun hash(payload: String): String = Sha256.hash(payload)

    @Deprecated("Use Sha256.hash", ReplaceWith("Sha256.hash(payload)", "org.http4k.security.Sha256"))
    fun hash(payload: ByteArray): String = Sha256.hash(payload)

    @Deprecated("Use Sha256.hmac", ReplaceWith("Sha256.hmac(key, data)", "org.http4k.security.Sha256"))
    fun hmacSHA256(key: ByteArray, data: String): ByteArray = Sha256.hmac(key, data)
}
