package org.http4k.security.jwt

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.util.Date

class RsaProvider(val issuer: String) {

    private val keyPair = KeyPairGenerator.getInstance("RSA")
        .apply { initialize(2048) }
        .generateKeyPair()

    val publicKey: RSAPublicKey get() = keyPair.public as RSAPublicKey

    fun generate(
        subject: String,
        vararg extraClaims: Pair<String, Any>,
        audience: List<String> = emptyList(),
        expires: Instant? = null
    ): String {
        val header = JWSHeader.Builder(JWSAlgorithm.RS256).build()
        val claims = JWTClaimsSet.Builder()
            .audience(audience)
            .issuer(issuer)
            .subject(subject)
            .expirationTime(expires?.let(Date::from))
            .apply { extraClaims.forEach { claim(it.first, it.second) }}
            .build()

        return SignedJWT(header, claims)
            .apply { sign(RSASSASigner(keyPair.private)) }
            .serialize()
    }
}
