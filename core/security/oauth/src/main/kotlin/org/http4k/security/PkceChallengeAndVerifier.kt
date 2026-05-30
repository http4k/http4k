package org.http4k.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.Random

data class PkceChallengeAndVerifier(val challenge: String, val verifier: String) {
    companion object {
        fun create(random: Random = SecureRandom(), verifierLength: Int = 128): PkceChallengeAndVerifier {
            // See https://www.oauth.com/oauth2-servers/pkce/authorization-request
            val codeVerifier = buildString {
                val codeChars = ('A'..'Z') + ('a'..'z') + ('0'..'9') + listOf('-', '.', '_', '~')
                repeat(verifierLength) {
                    append(codeChars[random.nextInt(codeChars.size)])
                }
            }

            return PkceChallengeAndVerifier(codeVerifier.toS256Challenge(), codeVerifier)
        }
    }
}

internal fun String.toS256Challenge(): String = toByteArray()
    .let { MessageDigest.getInstance("SHA-256").digest(it) }
    .let { String(Base64.getEncoder().encode(it)) }
    .replace("+", "-").replace("/", "_").trimEnd('=')

typealias PkceGenerator = () -> PkceChallengeAndVerifier
