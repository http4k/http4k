package org.http4k.security.oauth.client

import org.http4k.security.AccessToken
import org.http4k.security.ExpiringCredentials
import org.http4k.security.oauth.core.RefreshToken

interface AccessTokens {
    operator fun get(refreshToken: RefreshToken): ExpiringCredentials<AccessToken>?
    operator fun set(refreshToken: RefreshToken, accessToken: ExpiringCredentials<AccessToken>)
    companion object
}

fun AccessTokens.Companion.None() = object : AccessTokens {
    override fun get(refreshToken: RefreshToken): Nothing? = null
    override fun set(refreshToken: RefreshToken, accessToken: ExpiringCredentials<AccessToken>) {}
}

fun AccessTokens.Companion.InMemory() = object : AccessTokens {
    var tokens = mutableMapOf<RefreshToken, ExpiringCredentials<AccessToken>>()

    override fun get(refreshToken: RefreshToken) = tokens[refreshToken]

    override fun set(refreshToken: RefreshToken, accessToken: ExpiringCredentials<AccessToken>) {
        tokens[refreshToken] = accessToken
    }
}
