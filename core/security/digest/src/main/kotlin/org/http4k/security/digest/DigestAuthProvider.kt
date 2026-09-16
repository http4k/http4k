package org.http4k.security.digest

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.security.NonceGenerator
import org.http4k.security.NonceVerifier
import org.http4k.security.digest.DigestMode.Standard
import org.http4k.security.digest.Qop.AuthInt
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
    private val algorithm: DigestAlgorithm,
    private val nonceGenerator: NonceGenerator,
    private val nonceVerifier: NonceVerifier,
    private val digestMode: DigestMode = Standard
) {

    fun digestCredentials(request: Request) = request
        .header(digestMode.authHeaderName)
        ?.let { DigestCredential.fromHeader(it) }

    /**
     * Note: when the credentials carry Qop.AuthInt, the request entity body is read fully into memory to verify the digest
     */
    fun verify(credentials: DigestCredential, request: Request): Boolean {
        if (!pertainsToProvider(credentials, request)) return false
        val password = passwordLookup(credentials.username) ?: return false

        return MessageDigest.isEqual(credentials.responseBytes(), expectedDigest(credentials, request, password))
    }

    private fun pertainsToProvider(credentials: DigestCredential, request: Request) =
        (credentials.algorithm == null || credentials.algorithm == algorithm.value) &&
            credentials.realm == realm &&
            credentials.digestUri == request.uri.toString() &&
            nonceVerifier(credentials.nonce) &&
            when {
                qop.isEmpty() -> credentials.qop == null
                else -> credentials.qop != null && credentials.qop in qop
            }

    private fun expectedDigest(credentials: DigestCredential, request: Request, password: String) =
        DigestEncoder(MessageDigest.getInstance(algorithm.value))(
            method = request.method,
            realm = realm,
            qop = credentials.qop,
            username = credentials.username,
            password = password,
            nonce = credentials.nonce,
            cnonce = credentials.cnonce,
            nonceCount = credentials.nonceCount,
            digestUri = credentials.digestUri,
            entityBody = when (credentials.qop) {
                AuthInt -> request.body.entityBytes()
                else -> ByteArray(0)
            }
        )

    fun generateChallenge(): Response {
        val header = DigestChallenge(
            realm = realm,
            nonce = nonceGenerator(),
            algorithm = algorithm.value,
            qop = qop,
            opaque = null
        )

        return Response(UNAUTHORIZED).header(digestMode.challengeHeaderName, header.toHeaderValue())
    }
}
