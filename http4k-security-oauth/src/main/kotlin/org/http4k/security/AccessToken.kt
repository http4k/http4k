package org.http4k.security

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.annotation.JsonProperty
import org.http4k.core.Body
import org.http4k.format.Jackson.auto
import org.http4k.security.oauth.server.refreshtoken.RefreshToken
import org.http4k.security.openid.IdToken

data class AccessToken(val value: String,
                       val type: String? = "Bearer",
                       val expiresIn: Long? = null,
                       val scope: String? = null,
                       val refreshToken: RefreshToken? = null)

data class AccessTokenDetails(val accessToken: AccessToken, val idToken: IdToken? = null)

@JsonInclude(NON_NULL)
data class AccessTokenResponse(
    @JsonProperty("access_token") val access_token: String,
    @JsonProperty("token_type") val token_type: String? = null,
    @JsonProperty("expires_in") val expires_in: Long? = null,
    @JsonProperty("id_token") val id_token: String? = null,
    @JsonProperty("scope") val scope: String? = null,
    @JsonProperty("refresh_token") val refresh_token: String? = null
)

val accessTokenResponseBody = Body.auto<AccessTokenResponse>().toLens()
