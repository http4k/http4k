package org.http4k.security.oauth.server.refreshtoken

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.map
import org.http4k.core.Request
import org.http4k.security.AccessTokenDetails
import org.http4k.security.oauth.server.AccessTokenError
import org.http4k.security.oauth.server.ClientId
import org.http4k.security.oauth.server.InvalidRequest
import org.http4k.security.oauth.server.TokenRequest
import org.http4k.security.oauth.server.accesstoken.AccessTokenGenerator

class RefreshTokenAccessTokenGenerator(private val refreshTokens: RefreshTokens) : AccessTokenGenerator {
    override fun generate(request: Request, clientId: ClientId, tokenRequest: TokenRequest): Result<AccessTokenDetails, AccessTokenError> {
        val refreshToken = tokenRequest.refreshToken
            ?: return Failure(InvalidRequest("missing required parameter `refresh_token`"))
        return refreshTokens.refreshAccessToken(clientId, tokenRequest, refreshToken).map { AccessTokenDetails(it) }
    }
}
