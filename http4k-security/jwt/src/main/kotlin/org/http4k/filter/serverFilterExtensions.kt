package org.http4k.filter

import org.http4k.lens.RequestContextLens
import org.http4k.security.jwt.JwtAuthorizer

/**
 * Authorize requests containing a valid JWT as a Bearer token
 */
fun ServerFilters.JwtAuth(authorizer: JwtAuthorizer<*>) =
    ServerFilters.BearerAuth { authorizer(it) != null }

/**
 * Populate the request context for requests containing a valid JWT as a Bearer Token.
 */
fun <Principal: Any> ServerFilters.JwtAuth(
    authorizer: JwtAuthorizer<Principal>,
    principal: RequestContextLens<Principal>,
) = ServerFilters.BearerAuth(principal, authorizer)
