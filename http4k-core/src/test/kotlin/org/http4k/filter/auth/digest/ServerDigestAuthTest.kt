package org.http4k.filter.auth.digest

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isNullOrBlank
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.util.Hex
import org.junit.jupiter.api.Test
import java.security.MessageDigest

class ServerDigestAuthTest {

    companion object {
        private const val realm = "http4k"
    }

    private var nextNonce = "abcdefgh"
    private val nonceGenerator = object: NonceGenerator {
        override fun generate() = nextNonce
        override fun verify(nonce: String) = nonce == nextNonce
    }

    private val credentials = mapOf(
        "admin" to "password",
        "user" to "hunter2"
    )

    val digestCalculator = DigestCalculator(MessageDigest.getInstance("MD5"))

    private val handler = ServerFilters
        .DigestAuth(realm, { credentials[it] }, listOf(Qop.Auth), nonceGenerator = nonceGenerator)
        .then { Response(Status.OK) }

    @Test
    fun `no credentials - returns challenge`() {
        val response = handler(Request(Method.GET, "/"))

        assertThat(response.status, equalTo(Status.UNAUTHORIZED))
        assertThat(response.header("WWW-Authenticate"), equalTo("""Digest realm="$realm", nonce="abcdefgh", algorithm=MD5, qop="auth""""))
    }

    @Test
    fun `basic credentials - returns challenge`() {
        val request = Request(Method.GET, "/")
            .header("Authorization", "Basic hunter2")

        val response = handler(request)

        assertThat(response.status, equalTo(Status.UNAUTHORIZED))
        assertThat(response.header("WWW-Authenticate"), equalTo("""Digest realm="$realm", nonce="abcdefgh", algorithm=MD5, qop="auth""""))
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
            cnonce = "c123",
            qop = Qop.Auth,
            algorithm = "MD5",
            opaque = null
        )

        val request = Request(Method.GET, "/")
            .header("Authorization", credentials.toHeaderValue())

        val response = handler(request)

        assertThat(response.status, equalTo(Status.UNAUTHORIZED))
        assertThat(response.header("WWW-Authenticate"), isNullOrBlank)
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
            cnonce = "c123",
            qop = Qop.Auth,
            algorithm = "MD5",
            opaque = null
        )

        val request = Request(Method.GET, "/")
            .header("Authorization", credentials.toHeaderValue())

        val response = handler(request)

        assertThat(response.status, equalTo(Status.UNAUTHORIZED))
        assertThat(response.header("WWW-Authenticate"), isNullOrBlank)
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
            cnonce = "c123",
            qop = Qop.Auth,
            algorithm = "MD5",
            opaque = null
        )

        val request = Request(Method.GET, "/")
            .header("Authorization", credentials.toHeaderValue())

        val response = handler(request)

        assertThat(response.status, equalTo(Status.UNAUTHORIZED))
        assertThat(response.header("WWW-Authenticate"), isNullOrBlank)
    }

    @Test
    fun `digest credentials with mismatched method - returns 401`() {
        val credentials = DigestCredential(
            realm = "missing-realm",
            digestUri = "/",
            nonce = nextNonce,
            nonceCount = 1,
            response = Hex.hex(digestCalculator.encode(method = Method.POST, realm = realm, qop = Qop.Auth, username = "admin", password = "password", nonce = nextNonce, cnonce="c123", nonceCount = 1, digestUri = "/")),
            username = "admin",
            cnonce = "c123",
            qop = Qop.Auth,
            algorithm = "MD5",
            opaque = null
        )

        val request = Request(Method.GET, "/")
            .header("Authorization", credentials.toHeaderValue())

        val response = handler(request)

        assertThat(response.status, equalTo(Status.UNAUTHORIZED))
        assertThat(response.header("WWW-Authenticate"), isNullOrBlank)
    }

    @Test
    fun `digest credentials with mismatched password - returns 401`() {
        val credentials = DigestCredential(
            realm = realm,
            digestUri = "/",
            nonce = nextNonce,
            nonceCount = 1,
            response = Hex.hex(digestCalculator.encode(method = Method.GET, realm = realm, qop = Qop.Auth, username = "admin", password = "letmein", nonce = nextNonce, cnonce="c123", nonceCount = 1, digestUri = "/")),
            username = "admin",
            cnonce = "c123",
            qop = Qop.Auth,
            algorithm = "MD5",
            opaque = null
        )

        val request = Request(Method.GET, "/")
            .header("Authorization", credentials.toHeaderValue())

        val response = handler(request)

        assertThat(response.status, equalTo(Status.UNAUTHORIZED))
    }

    @Test
    fun `valid digest credentials - returns 200`() {
        val credentials = DigestCredential(
            realm = realm,
            digestUri = "/",
            nonce = nextNonce,
            nonceCount = 1,
            response = Hex.hex(digestCalculator.encode(method = Method.GET, realm = realm, qop = Qop.Auth, username = "admin", password = "password", nonce = nextNonce, cnonce="c123", nonceCount = 1, digestUri = "/")),
            username = "admin",
            cnonce = "c123",
            qop = Qop.Auth,
            algorithm = "MD5",
            opaque = null
        )

        val request = Request(Method.GET, "/")
            .header("Authorization", credentials.toHeaderValue())

        val response = handler(request)

        assertThat(response.status, equalTo(Status.OK))
    }
}
