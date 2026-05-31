package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.Response
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.with
import org.http4k.lens.RequestLens
import org.http4k.security.NonceGenerator
import org.http4k.security.NonceVerifier
import org.http4k.security.digest.DigestAuthProvider
import org.http4k.security.digest.DigestMode
import org.http4k.security.digest.DigestMode.Standard
import org.http4k.security.digest.Qop
import org.http4k.security.digest.Qop.Auth

fun ServerFilters.DigestAuth(
    realm: String,
    passwordLookup: (String) -> String?,
    qop: List<Qop> = listOf(Auth),
    digestMode: DigestMode = Standard,
    nonceGenerator: NonceGenerator,
    nonceVerifier: NonceVerifier,
    algorithm: String = "MD5",
    usernameKey: RequestLens<String>? = null,
): Filter {
    val provider = DigestAuthProvider(realm, passwordLookup, qop, algorithm, nonceGenerator, nonceVerifier, digestMode)
    return Filter { next ->
        filter@{ request ->
            val credentials = provider.digestCredentials(request) ?: return@filter provider.generateChallenge()
            if (!provider.verify(credentials, request.method, request.uri.toString())) return@filter Response(UNAUTHORIZED)

            next(usernameKey?.let { request.with(it of credentials.username) } ?: request)
        }
    }
}
