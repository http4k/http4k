package org.http4k.filter

import com.nimbusds.jwt.JWTClaimsSet
import org.http4k.core.Filter
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.with
import org.http4k.lens.Lens
import org.http4k.lens.RequestContextLens
import org.http4k.security.jwt.JwtAuthProvider

/**
 * Authorize requests with the given provider
 */
fun ServerFilters.JwtAuth(
    provider: JwtAuthProvider,
    tokenLens: Lens<Request, String?>
) = Filter { next ->
    { request ->
        tokenLens(request)
            ?.let(provider)
            ?.let { next(request) }
            ?: Response(UNAUTHORIZED)
    }
}

/**
 * Populate the principal with the given provider
 */
fun <Principal: Any> ServerFilters.JwtAuth(
    provider: JwtAuthProvider,
    tokenLens: Lens<Request, String?>,
    principal: RequestContextLens<Principal>,
    lookup: (JWTClaimsSet) -> Principal?,
) = Filter { next ->
    { request ->
        tokenLens(request)
            ?.let(provider)
            ?.let(lookup)
            ?.let { next(request.with(principal of it)) }
            ?: Response(UNAUTHORIZED)
    }
}
