package org.http4k.security.oauth.server

import org.http4k.security.AccessTokenContainer

/**
 * Provides a consistent way to generate access tokens
 */
interface AccessTokens {
    /**
     * Creates a new access token for a valid authorization code
     */
    fun create(authorizationCode: AuthorizationCode): AccessTokenContainer
}