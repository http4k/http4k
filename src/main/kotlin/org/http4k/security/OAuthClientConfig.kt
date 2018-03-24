package org.http4k.security

import org.http4k.core.Credentials
import org.http4k.core.Uri

data class OAuthClientConfig(
    val serviceName: String,
    private val authBase: Uri,
    val authPath: String,
    val apiBase: Uri,
    val tokenPath: String,
    val credentials: Credentials) {
    val authUri = authBase.path(authPath)
}