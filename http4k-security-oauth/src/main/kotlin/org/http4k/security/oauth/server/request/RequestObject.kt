package org.http4k.security.oauth.server.request

import com.fasterxml.jackson.annotation.JsonProperty
import org.http4k.core.Uri
import org.http4k.security.ResponseMode
import org.http4k.security.ResponseType
import org.http4k.security.State
import org.http4k.security.oauth.server.ClientId
import org.http4k.security.openid.Nonce

data class RequestObject(@JsonProperty("client_id") val client: ClientId? = null,
                         @JsonProperty("redirect_uri") val redirectUri: Uri? = null,
                         @JsonProperty("aud") val audience: String? = null,
                         @JsonProperty("iss") val issuer: String? = null,
                         @JsonProperty("scope") val scope: String? = null,
                         @JsonProperty("response_mode") val responseMode: ResponseMode? = null,
                         @JsonProperty("response_type") val responseType: ResponseType? = null,
                         @JsonProperty("state") val state: State? = null,
                         @JsonProperty("nonce") val nonce: Nonce? = null,
                         @JsonProperty("max_age") val magAge: Long? = null,
                         @JsonProperty("exp") val expiry: Long? = null,
                         @JsonProperty("claims") val claims: Claims = Claims()) {

    fun scopes(): List<String> = this.scope?.let { it.split(" ") } ?: emptyList()
}

data class Claims(@JsonProperty("userinfo") val userInfo: Map<String, Claim>? = null,
                  @JsonProperty("id_token") val idToken: Map<String, Claim>? = null)

data class Claim(val essential: Boolean = false, val value: String? = null, val values: List<String>? = null)
