package org.http4k.security.jwt

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jose.proc.SingleKeyJWSKeySelector
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import javax.crypto.spec.SecretKeySpec

class HsHelper(private val issuer: String, private val audience: Set<String>) {

    private val hsKey = SecretKeySpec("qwertyuiopasdfghjklzxcvbnm123456".toByteArray(), "HS256")
    private val provider = JwtAuthProvider(
        keySelector = SingleKeyJWSKeySelector(JWSAlgorithm.HS256, hsKey),
        audience = audience
    )

    fun generate(subject: String): String {
        val header = JWSHeader.Builder(JWSAlgorithm.HS256).build()
        val claims = JWTClaimsSet.Builder()
            .audience(audience.first())
            .issuer(issuer)
            .subject(subject)
            .build()

        return SignedJWT(header, claims)
            .apply { sign(MACSigner(hsKey)) }
            .serialize()
    }

    fun verify(token: String) = provider(token)
}
