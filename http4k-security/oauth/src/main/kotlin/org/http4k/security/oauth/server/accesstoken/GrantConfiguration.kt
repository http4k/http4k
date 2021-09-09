package org.http4k.security.oauth.server.accesstoken

enum class GrantType(val rfcValue: String) {
    AuthorizationCode("authorization_code"),
    ClientCredentials("client_credentials"),
    RefreshToken("refresh_token")
}

data class GrantTypesConfiguration(val supportedGrantTypes: Map<GrantType, AccessTokenRequestAuthentication>) {
    companion object {
        fun default(requestAuthentication: AccessTokenRequestAuthentication): GrantTypesConfiguration {
            return GrantTypesConfiguration(
                mapOf(
                    GrantType.AuthorizationCode to requestAuthentication,
                    GrantType.ClientCredentials to requestAuthentication,
                    GrantType.RefreshToken to requestAuthentication
                )
            )
        }
    }
}
