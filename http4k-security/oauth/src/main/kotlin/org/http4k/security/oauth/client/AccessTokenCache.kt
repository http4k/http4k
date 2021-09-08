package org.http4k.security.oauth.client

import org.http4k.security.oauth.core.RefreshToken

interface AccessTokenCache {
    operator fun get(refreshToken: RefreshToken): TokenData?
    operator fun set(refreshToken: RefreshToken, accessToken: TokenData)

    companion object {
        fun none() = object : AccessTokenCache {
            override fun get(refreshToken: RefreshToken): Nothing? = null
            override fun set(refreshToken: RefreshToken, accessToken: TokenData) {}
        }

        fun inMemory() = object : AccessTokenCache {
            var tokens = mutableMapOf<RefreshToken, TokenData>()

            override fun get(refreshToken: RefreshToken) = tokens[refreshToken]

            override fun set(refreshToken: RefreshToken, accessToken: TokenData) {
                tokens[refreshToken] = accessToken
            }
        }
    }
}
