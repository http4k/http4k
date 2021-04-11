package org.http4k.security

import org.http4k.core.Body
import org.http4k.security.oauth.server.OAuthServerMoshi.auto
import org.http4k.security.oauth.server.refreshtoken.RefreshToken
import org.http4k.security.openid.IdToken

data class AccessToken(val value: String,
                       val type: String? = "Bearer",
                       val expiresIn: Long? = null,
                       val scope: String? = null,
                       val refreshToken: RefreshToken? = null)

data class AccessTokenDetails(val accessToken: AccessToken, val idToken: IdToken? = null)

data class AccessTokenResponse(
     val access_token: String,
     val token_type: String? = null,
     val expires_in: Long? = null,
     val id_token: String? = null,
     val scope: String? = null,
     val refresh_token: String? = null
)

val accessTokenResponseBody = Body.auto<AccessTokenResponse>().toLens()
