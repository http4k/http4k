package org.http4k.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*
import kotlin.random.Random
import kotlin.random.asKotlinRandom

data class PkceChallengeAndVerifier(val challenge: String, val verifier: String) {
    companion object {
        val SECURE_PKCE = { create(SecureRandom().asKotlinRandom()) }

        fun create(random: Random, verifierLength: Int = 128): PkceChallengeAndVerifier {
            // See https://www.oauth.com/oauth2-servers/pkce/authorization-request
            val codeVerifier = buildString {
                val codeChars = ('A'..'Z') + ('a'..'z') + ('0'..'9') + listOf('-', '.', '_', '~')
                repeat(verifierLength) {
                    append(codeChars[random.nextInt(codeChars.size)])
                }
            }
            val codeChallenge = codeVerifier.toByteArray()
                .let { MessageDigest.getInstance("SHA-256").digest(it) }
                .let { String(Base64.getEncoder().encode(it)) }
                .replace("+", "-").replace("/", "_").trimEnd('=')

            return PkceChallengeAndVerifier(codeChallenge, codeVerifier)
        }
    }
}

typealias PkceGenerator = () -> PkceChallengeAndVerifier
