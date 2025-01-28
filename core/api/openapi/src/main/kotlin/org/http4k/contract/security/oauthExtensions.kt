package org.http4k.contract.security

import org.http4k.security.AuthCodeOAuthSecurity
import org.http4k.security.OAuthProvider
import org.http4k.security.OAuthScope
import org.http4k.security.OAuthSecurity

operator fun OAuthSecurity.Companion.invoke(
    oAuthProvider: OAuthProvider,
    customScopes: List<OAuthScope>? = null
) = AuthCodeOAuthSecurity(
    oAuthProvider.providerConfig.authUri,
    oAuthProvider.providerConfig.tokenUri,
    customScopes ?: oAuthProvider.scopes.map { OAuthScope(it, "") },
    oAuthProvider.authFilter
)
