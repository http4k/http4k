package org.http4k.security.oauth.server

import org.http4k.core.Uri
import org.http4k.security.oauth.server.accesstoken.GrantType
import org.http4k.security.oauth.server.refreshtoken.RefreshToken

data class TokenRequest(
    val grantType: GrantType,
    val clientId: ClientId?,
    val clientSecret: String?,
    val code: String?,
    val redirectUri: Uri?,
    val scopes: List<String>,
    val clientAssertionType: Uri?,
    val clientAssertion: String?,
    val refreshToken: RefreshToken?)
