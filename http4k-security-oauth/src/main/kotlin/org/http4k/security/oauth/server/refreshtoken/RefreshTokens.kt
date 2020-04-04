package org.http4k.security.oauth.server.refreshtoken

import org.http4k.security.AccessToken
import org.http4k.security.oauth.server.AccessTokenError
import org.http4k.security.oauth.server.ClientId
import org.http4k.security.oauth.server.TokenRequest
import org.http4k.security.oauth.server.UnsupportedGrantType
import org.http4k.security.oauth.server.accesstoken.GrantType
import org.http4k.util.Failure
import org.http4k.util.Result

interface RefreshTokens {
    fun refreshAccessToken(clientId: ClientId, tokenRequest: TokenRequest, refreshToken: RefreshToken): Result<AccessTokenError, AccessToken>

    companion object {
        val unsupported = object : RefreshTokens {
            override fun refreshAccessToken(clientId: ClientId, tokenRequest: TokenRequest, refreshToken: RefreshToken): Result<AccessTokenError, AccessToken> = Failure(UnsupportedGrantType(GrantType.RefreshToken.rfcValue))
        }
    }
}

data class RefreshToken(val value: String)
