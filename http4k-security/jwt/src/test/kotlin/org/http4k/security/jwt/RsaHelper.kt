package org.http4k.security.jwt

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyFactory
import java.security.KeyPair
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Date

class RsaHelper(
    private val clock: Clock,
    val issuer: String,
    val audience: Set<String> = emptySet(),
    private val duration: Duration = Duration.ofHours(1)
) {

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

    private val jwk = RSAKey.Builder(publicKey)
        .privateKey(keyPair.private as RSAPrivateKey)
        .keyUse(KeyUse.SIGNATURE)
        .keyID("key1")
        .issueTime(Date.from(clock.instant()))
        .build()
        .toPublicJWK()

    val http = routes(
        "keys.jwks" bind GET to { Response(OK).body("""{"keys":[$jwk]}""") }
    )

    fun generate(subject: String, vararg extraClaims: Pair<String, Any>, expires: Instant = clock.instant() + duration): String {
        val header = JWSHeader.Builder(JWSAlgorithm.RS256).build()
        val claims = JWTClaimsSet.Builder()
            .audience(audience.first())
            .issuer(issuer)
            .subject(subject)
            .expirationTime(Date.from(expires))
            .apply { extraClaims.forEach { claim(it.first, it.second) }}
            .build()

        return SignedJWT(header, claims)
            .apply { sign(RSASSASigner(keyPair.private)) }
            .serialize()
    }
}
