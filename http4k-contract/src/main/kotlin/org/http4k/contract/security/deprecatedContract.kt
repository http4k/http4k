package org.http4k.contract.security

import org.http4k.core.Filter
import org.http4k.core.Uri

@Deprecated("Renamed", ReplaceWith("AuthCodeOAuthSecurity(authorizationUrl, tokenUrl, scopes, filter, name, refreshUrl)"))
fun OAuthSecurity(
    authorizationUrl: Uri,
    tokenUrl: Uri,
    scopes: List<OAuthScope> = emptyList(),
    filter: Filter,
    name: String = "oauthSecurity"
) = AuthCodeOAuthSecurity(authorizationUrl, tokenUrl, scopes, filter, name)
