package org.http4k.security.oauth.server

import org.http4k.core.Uri

data class AuthRequest(
    val client: ClientId,
    val scopes: List<String>,
    val redirectUri: Uri,
    val state: String?
)

