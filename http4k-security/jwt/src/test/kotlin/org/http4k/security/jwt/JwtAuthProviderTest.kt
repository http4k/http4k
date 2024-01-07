package org.http4k.security.jwt

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.proc.SingleKeyJWSKeySelector
import com.nimbusds.jwt.JWTClaimsSet

import org.http4k.core.Uri
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class JwtAuthProviderTest {
    private val clock = object: Clock() {
        override fun instant() = Instant.parse("2024-01-07T12:00:00Z")
        override fun withZone(zone: ZoneId?) = TODO()
        override fun getZone() = ZoneOffset.UTC
    }

    private val rsa = RsaHelper(clock, "testServer", setOf("testApp"))

    @Test
    fun `process invalid jwt`() {
        val provider = JwtAuthProvider(
            keySelector = SingleKeyJWSKeySelector(JWSAlgorithm.RS256, rsa.publicKey),
            audience = rsa.audience,
            clock = clock
        )
        assertThat(provider("lolcats"), absent())
    }

    @Test
    fun `process local hs jwt`() {
        val hs = HsHelper( "testServer", setOf("testApp"))
        val token = hs.generate("sub1")
        val claims = hs.verify(token)
        assertThat(claims, present())
        assertThat(claims!!.subject, equalTo("sub1"))
    }

    @Test
    fun `process local rsa jwt`() {
        val token = rsa.generate("sub1")
        val provider = JwtAuthProvider(
            keySelector = SingleKeyJWSKeySelector(JWSAlgorithm.RS256, rsa.publicKey),
            audience = rsa.audience,
            clock = clock
        )

        val claims = provider(token)
        assertThat(claims, present())
        assertThat(claims!!.subject, equalTo("sub1"))
    }

    @Test
    fun `process remote jwt`() {
        val token = rsa.generate("sub1")
        val provider = JwtAuthProvider(
            keySelector = http4kJwsKeySelector(
                jwkUri = Uri.of("http://localhost/keys.jwks"),
                algorithm = JWSAlgorithm.RS256,
                http = rsa.http
            ),
            audience = rsa.audience,
            clock = clock
        )

        val claims = provider(token)
        assertThat(claims, present())
        assertThat(claims!!.subject, equalTo("sub1"))
    }

    @Test
    fun `verify issuer`() {
        val provider = JwtAuthProvider(
            keySelector = SingleKeyJWSKeySelector(JWSAlgorithm.RS256, rsa.publicKey),
            audience = rsa.audience,
            clock = clock,
            exactMatchClaims = JWTClaimsSet.Builder()
                .issuer(rsa.issuer)
                .build()
        )

        val token = rsa.generate("sub1")
        assertThat(provider(token), present())
    }

    @Test
    fun `verify issuer - invalid`() {
        val provider = JwtAuthProvider(
            keySelector = SingleKeyJWSKeySelector(JWSAlgorithm.RS256, rsa.publicKey),
            audience = rsa.audience,
            clock = clock,
            exactMatchClaims = JWTClaimsSet.Builder()
                .issuer("issuer2")
                .build()
        )

        val token = rsa.generate("sub1")
        assertThat(provider(token), absent())
    }

    @Test
    fun `verify extra claims`() {
        val provider = JwtAuthProvider(
            keySelector = SingleKeyJWSKeySelector(JWSAlgorithm.RS256, rsa.publicKey),
            exactMatchClaims = JWTClaimsSet.Builder()
                .claim("foo", "1")
                .build(),
            requiredClaims = setOf("bar"),
            prohibitedClaims = setOf("baz"),
            audience = rsa.audience,
            clock = clock
        )

        assertThat(provider(rsa.generate("sub1")), absent())
        assertThat(provider(rsa.generate("sub1", "foo" to "1")), absent())
        assertThat(provider(rsa.generate("sub1", "foo" to "2", "bar" to "1")), absent())
        assertThat(provider(rsa.generate("sub1", "foo" to "1", "bar" to "1")), present())
        assertThat(provider(rsa.generate("sub1", "foo" to "1", "bar" to "1", "baz" to "1")), absent())
    }

    @Test
    fun `verify expiry`() {
        val provider = JwtAuthProvider(
            keySelector = SingleKeyJWSKeySelector(JWSAlgorithm.RS256, rsa.publicKey),
            clock = clock,
            audience = rsa.audience
        )

        assertThat(rsa.generate("sub1", expires = clock.instant() - Duration.ofMinutes(1)).let(provider), absent())
        assertThat(rsa.generate("sub1", expires = clock.instant() + Duration.ofMinutes(1)).let(provider), present())
    }
}
