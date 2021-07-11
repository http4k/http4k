package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.then
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.http4k.security.Nonce
import org.http4k.security.NonceVerifier
import org.http4k.security.digest.DigestCalculator
import org.http4k.security.digest.DigestCredential
import org.http4k.security.digest.Qop
import org.http4k.util.Hex
import org.junit.jupiter.api.Test
import java.security.MessageDigest

class ServerDigestAuthTest {

    companion object {
        private const val realm = "http4k"
    }

    private var nextNonce = Nonce("abcdefgh")

    private val nonceGenerator = { nextNonce }
    private val nonceVerifier: NonceVerifier = { it == nextNonce }

    private val credentials = mapOf(
        "admin" to "password",
        "user" to "hunter2"
    )

    val digestCalculator = DigestCalculator(MessageDigest.getInstance("MD5"))

    private val handler = ServerFilters
        .DigestAuth(
            realm,
            { credentials[it] },
            listOf(Qop.Auth),
            nonceGenerator = nonceGenerator,
            nonceVerifier = nonceVerifier
        )
        .then { Response(OK) }

    @Test
    fun `no credentials - returns challenge`() {
        val response = handler(Request(GET, "/"))

        assertThat(response, hasStatus(UNAUTHORIZED))
        assertThat(
            response,
            hasHeader(
                "WWW-Authenticate",
                equalTo("""Digest realm="$realm", nonce="abcdefgh", algorithm=MD5, qop="auth"""")
            )
        )
    }

    @Test
    fun `basic credentials - returns challenge`() {
        val request = Request(GET, "/")
            .header("Authorization", "Basic hunter2")

        val response = handler(request)

        assertThat(response, hasStatus(UNAUTHORIZED))
        assertThat(
            response,
            hasHeader(
                "WWW-Authenticate",
                equalTo("""Digest realm="$realm", nonce="abcdefgh", algorithm=MD5, qop="auth"""")
            )
        )
    }

    @Test
    fun `digest credentials with invalid response - returns 401`() {
        val credentials = DigestCredential(
            realm = realm,
            digestUri = "/",
            nonce = nextNonce,
            nonceCount = 1,
            response = "abcdefgh",
            username = "admin",
            cnonce = Nonce("c123"),
            qop = Qop.Auth,
            algorithm = "MD5",
            opaque = null
        )

        val request = Request(GET, "/")
            .header("Authorization", credentials.toHeaderValue())

        val response = handler(request)

        assertThat(response, hasStatus(UNAUTHORIZED))
        assertThat(response, !hasHeader("WWW-Authenticate"))
    }

    @Test
    fun `digest credentials with invalid username - returns 401`() {
        val credentials = DigestCredential(
            realm = realm,
            digestUri = "/",
            nonce = nextNonce,
            nonceCount = 1,
            response = "abcdefgh",  // TODO valid response for another user
            username = "missingUser",
            cnonce = Nonce("c123"),
            qop = Qop.Auth,
            algorithm = "MD5",
            opaque = null
        )

        val request = Request(GET, "/")
            .header("Authorization", credentials.toHeaderValue())

        val response = handler(request)

        assertThat(response, hasStatus(UNAUTHORIZED))
        assertThat(response, !hasHeader("WWW-Authenticate"))
    }

    @Test
    fun `digest credentials with mismatched realm - returns 401`() {
        val credentials = DigestCredential(
            realm = "missing-realm",
            digestUri = "/",
            nonce = nextNonce,
            nonceCount = 1,
            response = "abcdefgh",  // TODO valid response
            username = "admin",
            cnonce = Nonce("c123"),
            qop = Qop.Auth,
            algorithm = "MD5",
            opaque = null
        )

        val request = Request(GET, "/")
            .header("Authorization", credentials.toHeaderValue())

        val response = handler(request)

        assertThat(response, hasStatus(UNAUTHORIZED))
        assertThat(response, !hasHeader("WWW-Authenticate"))
    }

    @Test
    fun `digest credentials with mismatched method - returns 401`() {
        val credentials = DigestCredential(
            realm = "missing-realm",
            digestUri = "/",
            nonce = nextNonce,
            nonceCount = 1,
            response = Hex.hex(
                digestCalculator.encode(
                    method = Method.POST,
                    realm = realm,
                    qop = Qop.Auth,
                    username = "admin",
                    password = "password",
                    nonce = nextNonce,
                    cnonce = Nonce("c123"),
                    nonceCount = 1,
                    digestUri = "/"
                )
            ),
            username = "admin",
            cnonce = Nonce("c123"),
            qop = Qop.Auth,
            algorithm = "MD5",
            opaque = null
        )

        val request = Request(GET, "/")
            .header("Authorization", credentials.toHeaderValue())

        val response = handler(request)

        assertThat(response, hasStatus(UNAUTHORIZED))
        assertThat(response, !hasHeader("WWW-Authenticate"))
    }

    @Test
    fun `digest credentials with mismatched password - returns 401`() {
        val credentials = DigestCredential(
            realm = realm,
            digestUri = "/",
            nonce = nextNonce,
            nonceCount = 1,
            response = Hex.hex(
                digestCalculator.encode(
                    method = GET,
                    realm = realm,
                    qop = Qop.Auth,
                    username = "admin",
                    password = "letmein",
                    nonce = nextNonce,
                    cnonce = Nonce("c123"),
                    nonceCount = 1,
                    digestUri = "/"
                )
            ),
            username = "admin",
            cnonce = Nonce("c123"),
            qop = Qop.Auth,
            algorithm = "MD5",
            opaque = null
        )

        val request = Request(GET, "/")
            .header("Authorization", credentials.toHeaderValue())

        val response = handler(request)

        assertThat(response, hasStatus(UNAUTHORIZED))
    }

    @Test
    fun `valid digest credentials - returns 200`() {
        val credentials = DigestCredential(
            realm = realm,
            digestUri = "/",
            nonce = nextNonce,
            nonceCount = 1,
            response = Hex.hex(
                digestCalculator.encode(
                    method = GET,
                    realm = realm,
                    qop = Qop.Auth,
                    username = "admin",
                    password = "password",
                    nonce = nextNonce,
                    cnonce = Nonce("c123"),
                    nonceCount = 1,
                    digestUri = "/"
                )
            ),
            username = "admin",
            cnonce = Nonce("c123"),
            qop = Qop.Auth,
            algorithm = "MD5",
            opaque = null
        )

        val request = Request(GET, "/")
            .header("Authorization", credentials.toHeaderValue())

        val response = handler(request)

        assertThat(response, hasStatus(OK))
    }
}
