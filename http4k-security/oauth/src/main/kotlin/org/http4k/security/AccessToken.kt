package org.http4k.security

import org.http4k.security.oauth.core.RefreshToken
import org.http4k.security.openid.IdToken

/**
 * Base interface for AccessToken. Implement this in concert with AccessTokenExtractor to provide a custom
 * implementation
 */
interface AccessToken {
    val value: String
    val type: String?
    val expiresIn: Long?
    val scope: String?
    val refreshToken: RefreshToken?

    companion object {
        operator fun invoke(
            value: String,
            type: String? = "Bearer",
            expiresIn: Long? = null,
            scope: String? = null,
            refreshToken: RefreshToken? = null
        ): AccessToken = BasicAccessToken(value, type, expiresIn, scope, refreshToken)
    }
}

private data class BasicAccessToken(
    override val value: String,
    override val type: String?,
    override val expiresIn: Long?,
    override val scope: String?,
    override val refreshToken: RefreshToken?
) : AccessToken

data class AccessTokenDetails(val accessToken: AccessToken, val idToken: IdToken? = null)

data class AccessTokenResponse(
    val access_token: String,
    val token_type: String? = null,
    val expires_in: Long? = null,
    val id_token: String? = null,
    val scope: String? = null,
    val refresh_token: String? = null
) {
    fun toAccessToken() =
        AccessToken(
            access_token,
            type = token_type ?: "Bearer",
            expiresIn = expires_in,
            scope = scope,
            refreshToken = refresh_token?.let(::RefreshToken)
        )
}
