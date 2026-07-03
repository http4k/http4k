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
import org.http4k.security.digest.DigestAlgorithm
import org.http4k.security.digest.DigestCredential
import org.http4k.security.digest.DigestEncoder
import org.http4k.security.digest.Qop
import org.http4k.util.Hex
import org.junit.jupiter.api.Test
import java.security.MessageDigest

private const val REALM = "http4k"

class ServerDigestAuthTest {

    private var nextNonce = Nonce("abcdefgh")

    private val nonceGenerator = { nextNonce }
    private val nonceVerifier: NonceVerifier = { it == nextNonce }

    private val credentials = mapOf(
        "admin" to "password",
        "user" to "hunter2"
    )

    val digestEncoder = DigestEncoder(MessageDigest.getInstance("MD5"))

    private val handler = ServerFilters
        .DigestAuth(
            REALM,
            { credentials[it] },
            listOf(Qop.Auth),
            nonceGenerator = nonceGenerator,
            nonceVerifier = nonceVerifier,
            algorithm = DigestAlgorithm.MD5
        )
        .then { Response(OK) }

    private val sha256Handler = ServerFilters
        .DigestAuth(
            REALM,
            { credentials[it] },
            listOf(Qop.Auth),
            nonceGenerator = nonceGenerator,
            nonceVerifier = nonceVerifier,
            algorithm = DigestAlgorithm.SHA_256
        )
        .then { Response(OK) }

    @Test
    fun `SHA-256 algorithm produces a SHA-256 challenge`() {
        val response = sha256Handler(Request(GET, "/"))

        assertThat(response, hasStatus(UNAUTHORIZED))
        assertThat(
            response,
            hasHeader(
                "WWW-Authenticate",
                equalTo("""Digest realm="$REALM", nonce="abcdefgh", algorithm=SHA-256, qop="auth"""")
            )
        )
    }

    @Test
    fun `SHA-256 valid credentials - returns 200`() {
        val sha256Encoder = DigestEncoder(MessageDigest.getInstance("SHA-256"))
        val credentials = DigestCredential(
            realm = REALM,
            digestUri = "/",
            nonce = nextNonce,
            nonceCount = 1,
            response = Hex.hex(
                sha256Encoder(
                    method = GET,
                    realm = REALM,
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
            algorithm = "SHA-256",
            opaque = null
        )

        val request = Request(GET, "/")
            .header("Authorization", credentials.toHeaderValue())

        assertThat(sha256Handler(request), hasStatus(OK))
    }

    @Test
    fun `no credentials - returns challenge`() {
        val response = handler(Request(GET, "/"))

        assertThat(response, hasStatus(UNAUTHORIZED))
        assertThat(
            response,
            hasHeader(
                "WWW-Authenticate",
                equalTo("""Digest realm="$REALM", nonce="abcdefgh", algorithm=MD5, qop="auth"""")
            )
        )
    }

    @Test
    fun `malformed Authorization header with no parameters - returns challenge instead of 500`() {
        val response = handler(Request(GET, "/").header("Authorization", "Digest"))

        assertThat(response, hasStatus(UNAUTHORIZED))
        assertThat(
            response,
            hasHeader(
                "WWW-Authenticate",
                equalTo("""Digest realm="$REALM", nonce="abcdefgh", algorithm=MD5, qop="auth"""")
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
                equalTo("""Digest realm="$REALM", nonce="abcdefgh", algorithm=MD5, qop="auth"""")
            )
        )
    }

    @Test
    fun `digest credentials with invalid response - returns 401`() {
        val credentials = DigestCredential(
            realm = REALM,
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
            realm = REALM,
            digestUri = "/",
            nonce = nextNonce,
            nonceCount = 1,
            response = "abcdefgh", // TODO valid response for another user
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
            response = "abcdefgh", // TODO valid response
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
                digestEncoder(
                    method = Method.POST,
                    realm = REALM,
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
            realm = REALM,
            digestUri = "/",
            nonce = nextNonce,
            nonceCount = 1,
            response = Hex.hex(
                digestEncoder(
                    method = GET,
                    realm = REALM,
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
    fun `digest credentials whose uri does not match request uri - returns 401`() {
        val credentials = DigestCredential(
            realm = REALM,
            digestUri = "/public",
            nonce = nextNonce,
            nonceCount = 1,
            response = Hex.hex(
                digestEncoder(
                    method = GET,
                    realm = REALM,
                    qop = Qop.Auth,
                    username = "admin",
                    password = "password",
                    nonce = nextNonce,
                    cnonce = Nonce("c123"),
                    nonceCount = 1,
                    digestUri = "/public"
                )
            ),
            username = "admin",
            cnonce = Nonce("c123"),
            qop = Qop.Auth,
            algorithm = "MD5",
            opaque = null
        )

        val request = Request(GET, "/admin")
            .header("Authorization", credentials.toHeaderValue())

        val response = handler(request)

        assertThat(response, hasStatus(UNAUTHORIZED))
    }

    @Test
    fun `digest credentials whose uri is missing the host - returns 401`() {
        val credentials = DigestCredential(
            realm = REALM,
            digestUri = "/public",
            nonce = nextNonce,
            nonceCount = 1,
            response = Hex.hex(
                digestEncoder(
                    method = GET,
                    realm = REALM,
                    qop = Qop.Auth,
                    username = "admin",
                    password = "password",
                    nonce = nextNonce,
                    cnonce = Nonce("c123"),
                    nonceCount = 1,
                    digestUri = "/public"
                )
            ),
            username = "admin",
            cnonce = Nonce("c123"),
            qop = Qop.Auth,
            algorithm = "MD5",
            opaque = null
        )

        val response = Request(GET, "http://server:80/public")
            .header("Authorization", credentials.toHeaderValue())
            .let(handler)

        assertThat(response, hasStatus(UNAUTHORIZED))
    }

    @Test
    fun `valid digest credentials - returns 200`() {
        val credentials = DigestCredential(
            realm = REALM,
            digestUri = "/",
            nonce = nextNonce,
            nonceCount = 1,
            response = Hex.hex(
                digestEncoder(
                    method = GET,
                    realm = REALM,
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
