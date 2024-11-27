package org.http4k.filter

import org.http4k.core.Credentials
import org.http4k.core.Filter
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.security.Nonce.Companion.SECURE_NONCE
import org.http4k.security.NonceGenerator
import org.http4k.security.digest.DigestAuthReceiver
import org.http4k.security.digest.DigestMode

fun ClientFilters.DigestAuth(
    credentials: Credentials,
    nonceGenerator: NonceGenerator = SECURE_NONCE,
    digestMode: DigestMode = DigestMode.Standard
) =
    DigestAuth({ credentials }, nonceGenerator, digestMode)

fun ClientFilters.DigestAuth(
    credentials: () -> Credentials,
    nonceGenerator: NonceGenerator,
    digestMode: DigestMode = DigestMode.Standard
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
