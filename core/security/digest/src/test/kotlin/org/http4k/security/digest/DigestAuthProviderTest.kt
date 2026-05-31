package org.http4k.security.digest

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.security.Nonce
import org.http4k.security.digest.Qop.Auth
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
        roundTrip("SHA-256")
    }

    @Test
    fun `verifies credentials computed with MD5 algorithm`() {
        roundTrip("MD5")
    }

    private fun roundTrip(algorithm: String) {
        val provider = DigestAuthProvider(
            realm = realm,
            passwordLookup = { if (it == username) password else null },
            qop = listOf(Auth),
            algorithm = algorithm,
            nonceGenerator = { nonce },
            nonceVerifier = { it == nonce }
        )

        val responseBytes = DigestEncoder(MessageDigest.getInstance(algorithm))(
            realm = realm,
            qop = Auth,
            method = GET,
            username = username,
            password = password,
            nonce = nonce,
            cnonce = cnonce,
            nonceCount = nonceCount,
            digestUri = digestUri
        )

        val credentials = DigestCredential(
            realm = realm,
            username = username,
            digestUri = digestUri,
            nonce = nonce,
            response = hex(responseBytes),
            opaque = null,
            nonceCount = nonceCount,
            algorithm = algorithm,
            cnonce = cnonce,
            qop = Auth
        )

        assertThat(provider.verify(credentials, GET), equalTo(true))
    }
}
