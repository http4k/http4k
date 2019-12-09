package org.http4k.security

import com.fasterxml.jackson.annotation.JsonProperty
import org.http4k.core.Body
import org.http4k.format.Jackson.auto
import org.http4k.security.openid.IdToken

data class AccessToken(val value: String)

data class AccessTokenDetails(val accessToken: AccessToken, val idToken: IdToken? = null, val scope: String? = null)

data class AccessTokenResponse(
    @JsonProperty("access_token") val accessToken: String,
    @JsonProperty("id_token") val idToken: String? = null,
    @JsonProperty("scope") val scope: String? = null
)

val accessTokenResponseBody = Body.auto<AccessTokenResponse>().toLens()