package org.http4k.security.oauth.server.refreshtoken

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import org.http4k.security.AccessToken
import org.http4k.security.oauth.server.AccessTokenError
import org.http4k.security.oauth.server.ClientId
import org.http4k.security.oauth.server.TokenRequest
import org.http4k.security.oauth.server.UnsupportedGrantType
import org.http4k.security.oauth.server.accesstoken.GrantType

fun interface RefreshTokens {
    fun refreshAccessToken(clientId: ClientId, tokenRequest: TokenRequest, refreshToken: RefreshToken): Result<AccessToken, AccessTokenError>

    companion object {
        val unsupported = RefreshTokens { _, _, _ -> Failure(UnsupportedGrantType(GrantType.RefreshToken.rfcValue)) }
    }
}

data class RefreshToken(val value: String)
