package org.http4k.security.oauth.server

import org.http4k.security.AccessToken
import org.http4k.security.oauth.server.accesstoken.AuthorizationCodeAccessTokenRequest
import org.http4k.util.Result

/**
 * Provides a consistent way to generate access tokens.
 */
interface AccessTokens {
    /**
     * Creates a new access token for a valid authorization code.
     */
    fun create(clientId: ClientId, tokenRequest: AuthorizationCodeAccessTokenRequest, authorizationCode: AuthorizationCode): Result<AuthorizationCodeAlreadyUsed, AccessToken>

    /**
     * creates a new access token for a given client.
     */
    fun create(clientId: ClientId, tokenRequest: TokenRequest): Result<AccessTokenError, AccessToken>
}
