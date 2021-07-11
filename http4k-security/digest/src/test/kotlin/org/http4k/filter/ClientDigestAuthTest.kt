package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isNullOrBlank
import org.http4k.core.Credentials
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.then
import org.http4k.security.Nonce
import org.http4k.security.NonceGeneratorVerifier
import org.http4k.security.digest.DigestChallenge
import org.http4k.security.digest.Qop.Auth
import org.junit.jupiter.api.Test

class ClientDigestAuthTest {

    private var nextNonce = Nonce("c1234")

    private val nonceGeneratorVerifier = object : NonceGeneratorVerifier {
        override fun invoke(nonce: Nonce) = nonce == nextNonce

        override fun invoke(): Nonce = nextNonce
    }

    @Test
    fun `ignore if no challenge`() {
        // handler returns Authorization header as body
        val handler: HttpHandler = { Response(OK).body(it.header("Authorization") ?: "") }

        val response = ClientFilters.DigestAuth(Credentials("user", "password"))
            .then(handler)(Request(Method.GET, "/"))

        // ensure no Authorization header was send by the client filter
        assertThat(response.bodyString(), isNullOrBlank)
    }

    @Test
    fun `ignore if 401 with no challenge`() {
        // handler returns Authorization header as body
        val handler: HttpHandler = { Response(UNAUTHORIZED).body(it.header("Authorization") ?: "") }

        val response = ClientFilters.DigestAuth(Credentials("user", "password"))
            .then(handler)(Request(Method.GET, "/"))

        // ensure no Authorization header was send by the client filter
        assertThat(response.bodyString(), isNullOrBlank)
    }

    @Test
    fun `respond to challenge and verify digest`() {
        val handler: HttpHandler = { request ->
            if (request.header("Authorization") == null) {
                // if no authorization given, present challenge
                val challenge = DigestChallenge(
                    realm = "http4k",
                    nonce = Nonce("1234abcd"),
                    algorithm = "MD5",
                    qop = listOf(Auth),
                    opaque = null
                )
                Response(UNAUTHORIZED).header("WWW-Authenticate", challenge.toHeaderValue())
            } else {
                // if authorization given, return it in body
                Response(OK).body(request.header("Authorization") ?: "")
            }
        }

        val response = ClientFilters.DigestAuth(Credentials("user", "password"), nonceGeneratorVerifier)
            .then(handler)(Request(Method.GET, "/"))

        assertThat(response.status, equalTo(OK))
        // ensure the client sent an Authorization digest, and verify it is consistent given a consistent nonce and cnonce
        assertThat(
            response.bodyString(),
            equalTo("Digest realm=\"http4k\", username=\"user\", uri=\"/\", nonce=\"1234abcd\", response=\"92582b92a7eacede09d20533466616e8\", nc=00000001, algorithm=MD5, cnonce=\"c1234\", qop=\"auth\"")
        )
    }
}
