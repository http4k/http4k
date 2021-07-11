package org.http4k.security.digest

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.security.NonceGenerator
import org.http4k.security.NonceVerifier
import org.http4k.security.digest.ParameterizedHeader.Companion.toParameterizedHeader
import java.security.MessageDigest

/**
 * For use in servers.  Verifies digest credentials and generates challenge responses
 *
 * TODO add support for opaque in challenge.  Unknown if it needs to be verified, or how it should be generated (i.e. static, user-specific, etc.)
 * The IOT device I used for testing constantly returned the same opaque
 */

enum class DigestMode(val authHeaderName: String, val challengeHeaderName: String) {
    Standard("Authorization", "WWW-Authenticate"),
    Proxy("Proxy-Authorization", "Proxy-Authenticate")
}

class DigestAuthProvider(
    private val realm: String,
    private val passwordLookup: (String) -> String?,
    private val qop: List<Qop>,
    private val algorithm: String,
    private val nonceGenerator: NonceGenerator,
    private val nonceVerifier: NonceVerifier = { true },
    private val digestMode: DigestMode = DigestMode.Standard
) {

    fun digestCredentials(request: Request): DigestCredential? {
        val header = request.header(digestMode.authHeaderName)?.toParameterizedHeader() ?: return null
        return DigestCredential.fromHeader(header)
    }

    fun verify(credentials: DigestCredential, method: Method): Boolean {
        val digestEncoder = DigestEncoder(MessageDigest.getInstance("MD5"))

        // verify credentials pertain to this provider
        if (credentials.algorithm != null && credentials.algorithm != algorithm) return false
        if (credentials.realm != realm) return false
        if (!nonceVerifier(credentials.nonce)) return false
        if (qop.isNotEmpty() && (credentials.qop == null || credentials.qop !in qop)) return false
        if (qop.isEmpty() && credentials.qop != null) return false

        // verify credentials digest matches expected digest
        val password = passwordLookup(credentials.username) ?: return false
        val expectedDigest = digestEncoder(
            method = method,
            realm = realm,
            qop = credentials.qop,
            username = credentials.username,
            password = password,
            nonce = credentials.nonce,
            cnonce = credentials.cnonce,
            nonceCount = credentials.nonceCount,
            digestUri = credentials.digestUri
        )
        val incomingDigest = credentials.responseBytes()

        return MessageDigest.isEqual(incomingDigest, expectedDigest)
    }

    fun generateChallenge(): Response {
        val header = DigestChallenge(
            realm = realm,
            nonce = nonceGenerator(),
            algorithm = algorithm,
            qop = qop,
            opaque = null
        )

        return Response(UNAUTHORIZED).header(digestMode.challengeHeaderName, header.toHeaderValue())
    }
}
