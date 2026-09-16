package org.http4k.filter

import org.http4k.core.Credentials
import org.http4k.core.Filter
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.security.Nonce.Companion.SECURE_NONCE
import org.http4k.security.NonceGenerator
import org.http4k.security.digest.DigestAuthReceiver
import org.http4k.security.digest.DigestMode
import org.http4k.security.digest.DigestMode.Standard

/**
 * Responds to Digest authentication challenges (RFC 7616).
 *
 * Note: if the server challenges with qop=auth-int, the request entity body is read fully into memory to compute the digest.
 */
fun ClientFilters.DigestAuth(
    credentials: Credentials,
    nonceGenerator: NonceGenerator = SECURE_NONCE,
    digestMode: DigestMode = Standard
) =
    DigestAuth({ credentials }, nonceGenerator, digestMode)

fun ClientFilters.DigestAuth(
    credentials: () -> Credentials,
    nonceGenerator: NonceGenerator,
    digestMode: DigestMode = Standard
): Filter {
    val receiver = DigestAuthReceiver(nonceGenerator, digestMode)

    return Filter { next ->
        op@{ request ->
            // TODO cache header for pre-emptive authorization?
            val response = next(request)
            if (response.status != UNAUTHORIZED) return@op response

            val challenge = receiver.getChallengeHeader(response) ?: return@op response
            val withDigest = receiver.authorizeRequest(request, challenge, credentials())
            next(withDigest)
        }
    }
}
