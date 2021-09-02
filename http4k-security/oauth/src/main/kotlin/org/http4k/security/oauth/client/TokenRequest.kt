package org.http4k.security.oauth.client

import org.http4k.core.Uri
import org.http4k.security.oauth.core.ClientId
import org.http4k.security.oauth.core.RefreshToken

data class TokenRequest(
    val grant_type: String,
    val refresh_token: String?,
    val client_id: String?,
    val code: String?,
    val redirect_uri: String?
    ) {
    companion object {
        fun refreshToken(refreshToken: RefreshToken) = TokenRequest(
            grant_type = "refresh_token",
            refresh_token = refreshToken.value,
            client_id = null,
            code = null,
            redirect_uri = null
        )
        fun authorizationCode(code: String, redirectUri: Uri?, clientId: ClientId?) = TokenRequest(
            grant_type = "authorization_code",
            refresh_token = null,
            client_id = clientId?.value,
            code = code,
            redirect_uri = redirectUri?.toString()
        )
    }
}
