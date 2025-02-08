package org.http4k.security

fun AuthCodeOAuthSecurity(oAuthProvider: OAuthProvider, customScopes: List<OAuthScope>? = null) = AuthCodeOAuthSecurity(
    oAuthProvider.providerConfig.authUri,
    oAuthProvider.providerConfig.tokenUri,
    customScopes ?: oAuthProvider.scopes.map { OAuthScope(it, "") },
    oAuthProvider.authFilter
)
