package org.http4k.filter

import org.http4k.core.Credentials
import org.http4k.core.Filter
import org.http4k.core.Status
import org.http4k.security.digest.DigestAuthReceiver
import org.http4k.security.digest.GenerateOnlyNonceGenerator
import org.http4k.security.digest.NonceGenerator

fun ClientFilters.DigestAuth(credentials: Credentials, nonceGenerator: NonceGenerator = GenerateOnlyNonceGenerator()) =
    DigestAuth({ credentials }, nonceGenerator)

fun ClientFilters.DigestAuth(
    credentials: () -> Credentials,
    nonceGenerator: NonceGenerator = GenerateOnlyNonceGenerator()
): Filter {
    val receiver = DigestAuthReceiver(nonceGenerator, proxy = false)

    return Filter { next ->
        op@{ request ->
            // TODO cache header for pre-emptive authorization?
            val response = next(request)
            if (response.status != Status.UNAUTHORIZED) return@op response

            val challenge = receiver.getChallengeHeader(response) ?: return@op response
            val withDigest = receiver.authorizeRequest(request, challenge, credentials())
            next(withDigest)
        }
    }
}
