package org.http4k.connect.amazon.iamidentitycenter

import org.http4k.connect.amazon.iamidentitycenter.model.PKCEChallenge
import org.http4k.connect.amazon.iamidentitycenter.model.PKCECodeVerifier
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object PKCES256Generator {
    private val secureRandom = SecureRandom()

    fun generate(): Pair<PKCEChallenge, PKCECodeVerifier> {
        val randomBytes = ByteArray(32).apply(secureRandom::nextBytes)
        val codeVerifier = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes)
        val challenge =
            Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").apply {
                update(codeVerifier.toByteArray(Charsets.UTF_8))
            }.digest())
        return PKCEChallenge.of(challenge) to PKCECodeVerifier.of(codeVerifier)
    }

}

