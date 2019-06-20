package org.http4k.contract.security

import org.http4k.core.Filter
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.security.OAuthProvider

data class OAuthSecurity(
    val authorizationUrl: Uri,
    val tokenUrl: Uri,
    val scopes: List<OAuthScope> = emptyList(),
    override val filter: Filter,
    val name: String = "oauthSecurity"
) : Security {
    companion object {
        operator fun invoke(oAuthProvider: OAuthProvider,
                            authFilter: Filter = ServerFilters.BearerAuth { true },
                            customScopes: List<OAuthScope>? = null) = OAuthSecurity(
            oAuthProvider.providerConfig.authUri,
            oAuthProvider.providerConfig.tokenUri,
            customScopes ?: oAuthProvider.scopes.map { OAuthScope(it, "") },
            oAuthProvider.authFilter.then(authFilter)
        )
    }
}

data class OAuthScope(val name: String, val description: String = name)