package org.http4k.security.jwt

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.proc.SingleKeyJWSKeySelector
import com.nimbusds.jwt.JWTClaimsSet
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Uri
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class JwtAuthorizerTest {
    private val rsa = RsaProvider("testServer")

    @Test
    fun `process invalid jwt`() {
        val provider = JwtAuthorizer(
            keySelector = SingleKeyJWSKeySelector(JWSAlgorithm.RS256, rsa.publicKey)
        )
        assertThat(provider("lolcats"), absent())
    }

    @Test
    fun `process local HS jwt`() {
        val hs = HsProvider( "testServer")
        val token = hs.generate("sub1")
        val claims = hs.verify(token)
        assertThat(claims, present())
        assertThat(claims!!.subject, equalTo("sub1"))
    }

    @Test
    fun `get verified subject`() {
        val token = rsa.generate("sub1")
        val provider = JwtAuthorizer(
            keySelector = SingleKeyJWSKeySelector(JWSAlgorithm.RS256, rsa.publicKey)
        )

        val claims = provider(token)
        assertThat(claims, present())
        assertThat(claims!!.subject, equalTo("sub1"))
    }

    @Test
    fun `process remote jwt`() {
        val token = rsa.generate("sub1")
        val provider = JwtAuthorizer(
            keySelector = http4kJwsKeySelector(
                jwkUri = Uri.of("http://localhost/keys.jwks"),
                algorithm = JWSAlgorithm.RS256,
                http = jwkServer(
                    RSAKey.Builder(rsa.publicKey)
                        .keyUse(KeyUse.SIGNATURE)
                        .keyID("key1")
                        .build()
                        .toPublicJWK()
                )
            )
        )

        val claims = provider(token)
        assertThat(claims, present())
        assertThat(claims!!.subject, equalTo("sub1"))
    }

    @Test
    fun `process remote jwt - 404`() {
        val provider = JwtAuthorizer(
            keySelector = http4kJwsKeySelector(
                jwkUri = Uri.of("http://localhost/keys.jwks"),
                algorithm = JWSAlgorithm.RS256,
                http = { Response(NOT_FOUND) }
            )
        )

        assertThat(provider(rsa.generate("sub1")), absent())
    }

    @Test
    fun `verify issuer`() {
        val provider = JwtAuthorizer(
            keySelector = SingleKeyJWSKeySelector(JWSAlgorithm.RS256, rsa.publicKey),
            exactMatchClaims = JWTClaimsSet.Builder()
                .issuer(rsa.issuer)
                .build()
        )

        val token = rsa.generate("sub1")
        assertThat(provider(token), present())
    }

    @Test
    fun `verify audience`() {
        val provider = JwtAuthorizer(
            keySelector = SingleKeyJWSKeySelector(JWSAlgorithm.RS256, rsa.publicKey),
            audience = setOf("foo", "bar")
        )

        assertThat(rsa.generate("sub1", audience = emptyList()).let(provider), absent())
        assertThat(rsa.generate("sub1", audience = listOf("foo")).let(provider), present())
        assertThat(rsa.generate("sub1", audience = listOf("foo", "bar")).let(provider), present())
        assertThat(rsa.generate("sub1", audience = listOf("foo", "bar", "baz")).let(provider), present())
        assertThat(rsa.generate("sub1", audience = listOf("baz")).let(provider), absent())
    }

    @Test
    fun `verify issuer - invalid`() {
        val provider = JwtAuthorizer(
            keySelector = SingleKeyJWSKeySelector(JWSAlgorithm.RS256, rsa.publicKey),
            exactMatchClaims = JWTClaimsSet.Builder()
                .issuer("issuer2")
                .build()
        )

        val token = rsa.generate("sub1")
        assertThat(provider(token), absent())
    }

    @Test
    fun `verify extra claims`() {
        val provider = JwtAuthorizer(
            keySelector = SingleKeyJWSKeySelector(JWSAlgorithm.RS256, rsa.publicKey),
            exactMatchClaims = JWTClaimsSet.Builder()
                .claim("foo", "1")
                .build(),
            requiredClaims = setOf("bar"),
            prohibitedClaims = setOf("baz")
        )

        assertThat(provider(rsa.generate("sub1")), absent())
        assertThat(provider(rsa.generate("sub1", "foo" to "1")), absent())
        assertThat(provider(rsa.generate("sub1", "foo" to "2", "bar" to "1")), absent())
        assertThat(provider(rsa.generate("sub1", "foo" to "1", "bar" to "1")), present())
        assertThat(provider(rsa.generate("sub1", "foo" to "1", "bar" to "1", "baz" to "1")), absent())
    }

    @Test
    fun `verify expiry`() {
        val clock = object: Clock() {
            override fun instant() = Instant.parse("2024-01-07T12:00:00Z")
            override fun withZone(zone: ZoneId?) = TODO()
            override fun getZone() = ZoneOffset.UTC
        }

        val provider = JwtAuthorizer(
            keySelector = SingleKeyJWSKeySelector(JWSAlgorithm.RS256, rsa.publicKey),
            clock = clock
        )

        assertThat(rsa.generate("sub1", expires = clock.instant() - Duration.ofMinutes(1)).let(provider), absent())
        assertThat(rsa.generate("sub1", expires = clock.instant() + Duration.ofMinutes(1)).let(provider), present())
    }
}
