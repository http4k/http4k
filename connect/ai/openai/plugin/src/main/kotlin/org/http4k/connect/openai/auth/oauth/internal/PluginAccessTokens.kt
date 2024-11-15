package org.http4k.connect.openai.auth.oauth.internal

import dev.forkhandles.result4k.Failure
import org.http4k.connect.openai.auth.oauth.PrincipalStore
import org.http4k.connect.openai.auth.oauth.PrincipalTokens
import org.http4k.security.oauth.server.AccessTokens
import org.http4k.security.oauth.server.AuthorizationCodeAlreadyUsed
import org.http4k.security.oauth.server.ClientId
import org.http4k.security.oauth.server.TokenRequest
import org.http4k.security.oauth.server.UnsupportedGrantType
import org.http4k.security.oauth.server.accesstoken.AuthorizationCodeAccessTokenRequest

internal fun <Principal : Any> PluginAccessTokens(
    principalStore: PrincipalStore<Principal>,
    accessTokens: PrincipalTokens<Principal>
) = object : AccessTokens {
    override fun create(clientId: ClientId, tokenRequest: TokenRequest) =
        Failure(UnsupportedGrantType("client_credentials"))

    override fun create(
        clientId: ClientId,
        tokenRequest: AuthorizationCodeAccessTokenRequest
    ) = when (val principal = principalStore[tokenRequest.authorizationCode]) {
        null -> Failure(AuthorizationCodeAlreadyUsed)
        else -> accessTokens.create(principal)
    }
}
