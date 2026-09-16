package org.http4k.security.digest

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.security.Nonce
import org.http4k.security.digest.Qop.Auth
import org.http4k.security.digest.Qop.AuthInt
import org.http4k.util.Hex.hex
import org.junit.jupiter.api.Test
import java.security.MessageDigest

class DigestAuthProviderTest {

    private val realm = "test-realm"
    private val username = "alice"
    private val password = "s3cret"
    private val nonce = Nonce("nonce-1")
    private val cnonce = Nonce("cnonce-1")
    private val nonceCount = 1L
    private val digestUri = "/protected"

    @Test
    fun `verifies credentials computed with negotiated SHA-256 algorithm`() {
        roundTrip(DigestAlgorithm.SHA_256)
    }

    @Test
    fun `verifies credentials computed with MD5 algorithm`() {
        roundTrip(DigestAlgorithm.MD5)
    }

    private fun roundTrip(algorithm: DigestAlgorithm) {
        val provider = DigestAuthProvider(
            realm = realm,
            passwordLookup = { if (it == username) password else null },
            qop = listOf(Auth),
            algorithm = algorithm,
            nonceGenerator = { nonce },
            nonceVerifier = { it == nonce }
        )

        val responseBytes = DigestEncoder(MessageDigest.getInstance(algorithm.value))(
            realm = realm,
            qop = Auth,
            method = GET,
            username = username,
            password = password,
            nonce = nonce,
            cnonce = cnonce,
            nonceCount = nonceCount,
            digestUri = digestUri,
            entityBody = ByteArray(0)
        )

        val credentials = DigestCredential(
            realm = realm,
            username = username,
            digestUri = digestUri,
            nonce = nonce,
            response = hex(responseBytes),
            opaque = null,
            nonceCount = nonceCount,
            algorithm = algorithm.value,
            cnonce = cnonce,
            qop = Auth
        )

        assertThat(provider.verify(credentials, Request(GET, digestUri)), equalTo(true))
    }

    @Test
    fun `verifies auth-int credentials over the received entity body`() {
        val body = "amount=10&to=alice"
        val request = Request(POST, digestUri).body(body)

        assertThat(authIntProvider().verify(authIntCredentials(body.toByteArray(Charsets.ISO_8859_1)), request), equalTo(true))
    }

    @Test
    fun `rejects auth-int credentials when the entity body was tampered in flight`() {
        val signedBody = "amount=10&to=alice".toByteArray(Charsets.ISO_8859_1)
        val tamperedRequest = Request(POST, digestUri).body("amount=999999&to=attacker")

        assertThat(authIntProvider().verify(authIntCredentials(signedBody), tamperedRequest), equalTo(false))
    }

    private fun authIntProvider() = DigestAuthProvider(
        realm = realm,
        passwordLookup = { if (it == username) password else null },
        qop = listOf(AuthInt),
        algorithm = DigestAlgorithm.MD5,
        nonceGenerator = { nonce },
        nonceVerifier = { it == nonce }
    )

    private fun authIntCredentials(entityBody: ByteArray): DigestCredential {
        val responseBytes = DigestEncoder(MessageDigest.getInstance(DigestAlgorithm.MD5.value))(
            realm = realm,
            qop = AuthInt,
            method = POST,
            username = username,
            password = password,
            nonce = nonce,
            cnonce = cnonce,
            nonceCount = nonceCount,
            digestUri = digestUri,
            entityBody = entityBody
        )

        return DigestCredential(
            realm = realm,
            username = username,
            digestUri = digestUri,
            nonce = nonce,
            response = hex(responseBytes),
            opaque = null,
            nonceCount = nonceCount,
            algorithm = DigestAlgorithm.MD5.value,
            cnonce = cnonce,
            qop = AuthInt
        )
    }
}
