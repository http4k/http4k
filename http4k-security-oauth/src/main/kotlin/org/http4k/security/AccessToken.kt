package org.http4k.security

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.annotation.JsonProperty
import org.http4k.core.Body
import org.http4k.format.Jackson.auto
import org.http4k.security.oauth.server.refreshtoken.RefreshToken
import org.http4k.security.openid.IdToken

data class AccessToken(
    val value: String,
    val type: String? = "Bearer",
    val expiresIn: Long? = null,
    val scope: String? = null,
    val refreshToken: RefreshToken? = null
)

data class AccessTokenDetails(val accessToken: AccessToken, val idToken: IdToken? = null)

@JsonInclude(NON_NULL)
data class AccessTokenResponse(
    @JsonProperty("access_token") val accessToken: String,
    @JsonProperty("token_type") val tokenType: String? = null,
    @JsonProperty("expires_in") val expiresIn: Long? = null,
    @JsonProperty("id_token") val idToken: String? = null,
    @JsonProperty("scope") val scope: String? = null,
    @JsonProperty("refresh_token") val refreshToken: String? = null
)

val accessTokenResponseBody = Body.auto<AccessTokenResponse>().toLens()
