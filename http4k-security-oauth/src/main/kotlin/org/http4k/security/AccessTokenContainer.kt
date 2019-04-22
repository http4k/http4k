package org.http4k.security

import com.fasterxml.jackson.annotation.JsonProperty
import org.http4k.core.Body
import org.http4k.format.Jackson.auto

data class AccessTokenContainer(val value: String)

data class AccessTokenResponse(@JsonProperty("access_token") val accessToken: String)

val accessTokenResponseBody = Body.auto<AccessTokenResponse>().toLens()