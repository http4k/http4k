package org.http4k.security.oauth.server

import org.http4k.core.Filter
import org.http4k.core.Response
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.Uri
import org.http4k.filter.ServerFilters
import org.http4k.lens.bearerToken

/**
 * A filter that checks for a valid Bearer token in the Authorization header of incoming requests.
 */
fun ServerFilters.BearerAuthWithAuthServerDiscovery(
    realm: String,
    authServerUri: Uri,
    check: (String) -> Boolean
) = Filter { next ->
    {
        val body = Response(UNAUTHORIZED)
            .header(
                "WWW-Authenticate",
                """Bearer realm="$realm", error="invalid_token", auth_server="$authServerUri" """
            )
        when (val bearerToken = it.bearerToken()) {
            null -> body
            else -> if (check(bearerToken)) next(it) else body
        }
    }
}

fun ServerFilters.BearerAuthWithAuthServerDiscovery(
    realm: String,
    authServerUri: Uri,
    check: String
) = ServerFilters.BearerAuthWithAuthServerDiscovery(realm, authServerUri) { it == check }
