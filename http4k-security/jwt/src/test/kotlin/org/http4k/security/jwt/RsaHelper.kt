package org.http4k.security.jwt

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.proc.SingleKeyJWSKeySelector
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import kotlin.io.path.Path

class RsaHelper(private val issuer: String, private val audience: Set<String> = emptySet()) {

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

    init {
        val base64 = Base64.getUrlEncoder()
        println(base64.encodeToString(keyPair.public.encoded))
    }

    private val provider = JwtAuthProvider(
        keySelector = SingleKeyJWSKeySelector(JWSAlgorithm.RS256, keyPair.public),
        audience = audience
    )

    fun generate(subject: String): String {
        val header = JWSHeader.Builder(JWSAlgorithm.RS256).build()
        val claims = JWTClaimsSet.Builder()
            .audience(audience.first())
            .issuer(issuer)
            .subject(subject)
            .build()

        return SignedJWT(header, claims)
            .apply { sign(RSASSASigner(keyPair.private)) }
            .serialize()
    }

    fun verify(token: String) = provider(token)
}

class RsaKeyGenerator {
    @Test
    @Disabled
    fun `generate key pair`() {
        val keyPair = KeyPairGenerator.getInstance("RSA")
            .apply { initialize(2048) }
            .generateKeyPair()

        Files.write(Path("src/test/resources/keys/rsa_priv.key"), keyPair.private.encoded)
        Files.write(Path("src/test/resources/keys/rsa_pub.key"), keyPair.public.encoded)
    }
}
