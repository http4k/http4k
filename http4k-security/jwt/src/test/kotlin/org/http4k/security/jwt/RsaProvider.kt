package org.http4k.security.jwt

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyFactory
import java.security.KeyPair
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.time.Instant
import java.util.Date

class RsaProvider(val issuer: String) {

    private fun loadResource(name: String) = javaClass.classLoader.getResource(name)
        ?.toURI()!!
        .let(Paths::get)
        .let(Files::readAllBytes)

    private val keyPair = let {
        val kf = KeyFactory.getInstance("RSA")
        KeyPair(
            kf.generatePublic(X509EncodedKeySpec(loadResource("keys/rsa_pub.key"))),
            kf.generatePrivate(PKCS8EncodedKeySpec(loadResource("keys/rsa_priv.key"))),
        )
    }

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
