package org.http4k.connect.openai.auth.oauth

import dev.forkhandles.result4k.Result4k
import org.http4k.core.Request
import org.http4k.security.AccessToken
import org.http4k.security.oauth.server.AccessTokenError
import org.http4k.security.oauth.server.refreshtoken.RefreshTokens

/**
 * Creates OAuth tokens for the plugin. This can be a simple token or a JWT,
 * but the important thing is that the original Principal is resolvable from it.
 */
interface PrincipalTokens<Principal : Any> : RefreshTokens {
    fun resolve(accessToken: AccessToken): Principal?
    fun resolve(request: Request): Principal?
    fun create(principal: Principal): Result4k<AccessToken, AccessTokenError>
}
