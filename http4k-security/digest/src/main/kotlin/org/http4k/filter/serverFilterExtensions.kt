package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.Response
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.with
import org.http4k.lens.RequestContextLens
import org.http4k.security.digest.DigestAuthProvider
import org.http4k.security.digest.GenerateOnlyNonceGenerator
import org.http4k.security.digest.NonceGenerator
import org.http4k.security.digest.Qop

fun ServerFilters.DigestAuth(
    realm: String,
    passwordLookup: (String) -> String?,
    qop: List<Qop> = listOf(Qop.Auth),
    proxy: Boolean = false,
    nonceGenerator: NonceGenerator = GenerateOnlyNonceGenerator(),
    algorithm: String = "MD5",
    usernameKey: RequestContextLens<String>? = null,
): Filter {
    val provider = DigestAuthProvider(realm, passwordLookup, qop, proxy, nonceGenerator, algorithm)
    return Filter { next ->
        filter@{ request ->
            val credentials = provider.getDigestCredentials(request) ?: return@filter provider.generateChallenge()
            if (!provider.verify(credentials, request.method)) return@filter Response(UNAUTHORIZED)

            if (usernameKey != null) {
                next(request.with(usernameKey of credentials.username))
            } else {
                next(request)
            }
        }
    }
}
