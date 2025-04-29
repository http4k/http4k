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
fun ServerFilters.BearerAuthWithResourceMetadata(
    protectedResourceUri: Uri,
    vararg contents: Pair<String, String>,
    checkToken: (String) -> Boolean
) = Filter { next ->
    {
        val headerValue = (listOf("resource_metadata" to protectedResourceUri.toString()) + contents.toList())
            .joinToString(", ") { (k, v) -> "$k=\"$v\"" }
        val body = Response(UNAUTHORIZED)
            .header("WWW-Authenticate", "Bearer $headerValue")
        when (val bearerToken = it.bearerToken()) {
            null -> body
            else -> if (checkToken(bearerToken)) next(it) else body
        }
    }
}
