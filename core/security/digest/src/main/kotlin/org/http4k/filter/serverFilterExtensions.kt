package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.MemoryBody
import org.http4k.core.Response
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.with
import org.http4k.lens.RequestLens
import org.http4k.security.NonceGenerator
import org.http4k.security.NonceVerifier
import org.http4k.security.digest.DigestAlgorithm
import org.http4k.security.digest.DigestAuthProvider
import org.http4k.security.digest.DigestMode
import org.http4k.security.digest.DigestMode.Standard
import org.http4k.security.digest.Qop
import org.http4k.security.digest.Qop.Auth
import org.http4k.security.digest.Qop.AuthInt
import org.http4k.security.digest.entityBytes

/**
 * Protects routes with Digest authentication (RFC 7616).
 *
 * Note: when [Qop.AuthInt] is negotiated, the request entity body is read fully into memory to verify the digest
 */
fun ServerFilters.DigestAuth(
    realm: String,
    passwordLookup: (String) -> String?,
    qop: List<Qop> = listOf(Auth),
    digestMode: DigestMode = Standard,
    nonceGenerator: NonceGenerator,
    nonceVerifier: NonceVerifier,
    algorithm: DigestAlgorithm,
    usernameKey: RequestLens<String>? = null,
): Filter {
    val provider = DigestAuthProvider(realm, passwordLookup, qop, algorithm, nonceGenerator, nonceVerifier, digestMode)
    return Filter { next ->
        filter@{ request ->
            val credentials = provider.digestCredentials(request) ?: return@filter provider.generateChallenge()

            val verified = when (credentials.qop) {
                AuthInt -> request.body(MemoryBody(request.body.entityBytes()))
                else -> request
            }
            if (!provider.verify(credentials, verified)) return@filter Response(UNAUTHORIZED)

            next(usernameKey?.let { verified.with(it of credentials.username) } ?: verified)
        }
    }
}
