package org.http4k.security.oauth.server

import org.http4k.filter.ServerFilters
import org.http4k.security.AccessTokenContainer

fun AccessTokenValidationFilter(accessTokens: AccessTokens) = ServerFilters.BearerAuth {
    accessTokens.isValid(AccessTokenContainer(it))
}