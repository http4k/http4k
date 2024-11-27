package org.http4k.security.digest

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.security.Nonce
import org.junit.jupiter.api.Test

class DigestChallengeTest {

    @Test
    fun `process basic challenge for header with extra space`() {
        val challenge = DigestChallenge.parse(" Digest realm=\"server\", nonce=\"6a4363b9256176c8c225ee70d191a620\"")
        assertThat(challenge, equalTo(DigestChallenge(
            realm = "server",
            nonce = Nonce("6a4363b9256176c8c225ee70d191a620"),
            algorithm = null,
            opaque = null,
            qop = emptyList()
        )))
    }

    @Test
    fun `process auth challenge`() {
        val challenge = DigestChallenge.parse("Digest realm=\"http4k\", nonce=\"1234abcd\", algorithm=MD5, qop=\"auth\"")
        assertThat(challenge, equalTo(DigestChallenge(
            realm = "http4k",
            nonce = Nonce("1234abcd"),
            algorithm = "MD5",
            opaque = null,
            qop = listOf(Qop.Auth)
        )))
    }

    @Test
    fun `process auth challenge with '=' in nonce`() {
        val challenge = DigestChallenge.parse("Digest realm=\"axis\", nonce=\"1234=abcd\", algorithm=MD5, qop=\"auth\"")
        assertThat(challenge, equalTo(DigestChallenge(
            realm = "axis",
            nonce = Nonce("1234=abcd"),
            algorithm = "MD5",
            opaque = null,
            qop = listOf(Qop.Auth)
        )))
    }
}
