package org.http4k.security.oauth.server.accesstoken

import org.http4k.security.oauth.server.accesstoken.GrantType.*

enum class GrantType(val rfcValue: String) {
    AuthorizationCode("authorization_code"),
    ClientCredentials("client_credentials"),
    RefreshToken("refresh_token")
}

data class GrantTypesConfiguration(val supportedGrantTypes: Map<GrantType, AccessTokenRequestAuthentication>) {
    companion object {
        fun default(requestAuthentication: AccessTokenRequestAuthentication) =
            GrantTypesConfiguration(
                mapOf(
                    AuthorizationCode to requestAuthentication,
                    ClientCredentials to requestAuthentication,
                    RefreshToken to requestAuthentication
                )
            )
    }
}
