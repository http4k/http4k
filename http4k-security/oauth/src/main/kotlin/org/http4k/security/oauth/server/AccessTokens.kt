package org.http4k.security.oauth.server

import dev.forkhandles.result4k.Result
import org.http4k.security.AccessToken
import org.http4k.security.oauth.server.accesstoken.AuthorizationCodeAccessTokenRequest

/**
 * Provides a consistent way to generate access tokens.
 */
interface AccessTokens {
    /**
     * Creates a new access token for a valid authorization code.
     */
    fun create(
        clientId: ClientId,
        tokenRequest: AuthorizationCodeAccessTokenRequest,
    ): Result<AccessToken, AccessTokenError>

    @Deprecated(
        "AuthorizationCode is already present in the tokenRequest, so use that",
        ReplaceWith("create(clientId, tokenRequest: AuthorizationCodeAccessTokenRequest)")
    )
    fun create(
        clientId: ClientId,
        tokenRequest: AuthorizationCodeAccessTokenRequest,
        authorizationCode: AuthorizationCode
    ): Result<AccessToken, AccessTokenError> = create(clientId, tokenRequest)

    /**
     * creates a new access token for a given client.
     */
    fun create(clientId: ClientId, tokenRequest: TokenRequest): Result<AccessToken, AccessTokenError>
}
