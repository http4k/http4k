package org.http4k.security.oauth.server.refreshToken

import com.natpryce.Failure
import com.natpryce.Result
import org.http4k.security.AccessToken
import org.http4k.security.oauth.server.AccessTokenError
import org.http4k.security.oauth.server.UnsupportedGrantType
import org.http4k.security.oauth.server.accesstoken.GrantType

interface RefreshTokens {
    fun refreshAccessToken(refreshToken: RefreshToken): Result<AccessToken, AccessTokenError>

    companion object {
        val unsupported = object : RefreshTokens {
            override fun refreshAccessToken(refreshToken: RefreshToken): Result<AccessToken, AccessTokenError> = Failure(UnsupportedGrantType(GrantType.RefreshToken.rfcValue))
        }
    }
}

data class RefreshToken(val value: String)
