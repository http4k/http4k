package org.http4k.security.digest

import org.http4k.core.Method
import org.http4k.security.Nonce
import org.http4k.security.digest.Qop.Auth
import org.http4k.security.digest.Qop.AuthInt
import org.http4k.util.Hex
import java.nio.charset.Charset
import java.security.MessageDigest
import kotlin.text.Charsets.ISO_8859_1

class DigestEncoder(private val digester: MessageDigest, private val charset: Charset = ISO_8859_1) {

    private fun hexDigest(value: String) = Hex.hex(digest(value))
    private fun digest(value: String) = digester.digest(value.toByteArray(charset))

    operator fun invoke(
        realm: String,
        qop: Qop?,
        method: Method,
        username: String,
        password: String,
        nonce: Nonce,
        cnonce: Nonce?,
        nonceCount: Long?,
        digestUri: String
    ): ByteArray {
        val nc = nonceCount?.toString(16)?.padStart(8, '0')

        /*
         *  TODO: If the algorithm directive's value is "MD5-sess", then HA1 is
         * HA1 = MD5(MD5(username:realm:password):nonce:cnonce)
         * Note: this feature doesn't have wide browser compatibility, so may be ok to ignore
         */
        val ha1 = hexDigest("${username}:$realm:$password")

        /*
        * TODO auth-int QoP should be of format MD5(method:digestURI:MD5(entityBody))
        * This might be problematic if BodyMode is Stream
        * Note: this feature doesn't have wide browser compatibility, so may be ok to ignore
        */
        val ha2 = hexDigest("$method:$digestUri")

        val response = when (qop) {
            null -> "$ha1:$nonce:$ha2"
            Auth, AuthInt -> "$ha1:$nonce:$nc:$cnonce:${qop.value}:$ha2"
        }
        return digest(response)
    }
}
