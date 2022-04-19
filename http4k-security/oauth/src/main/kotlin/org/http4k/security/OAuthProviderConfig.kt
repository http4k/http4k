package org.http4k.security

import org.http4k.core.Credentials
import org.http4k.core.Uri
import org.http4k.core.extend

data class OAuthProviderConfig(
    private val authBase: Uri,
    val authPath: String,
    val tokenPath: String,
    val credentials: Credentials,
    val apiBase: Uri = authBase,
    val authUri: Uri = authBase.extend(Uri.of(authPath)),
    val tokenUri: Uri = authBase.extend(Uri.of(tokenPath)))
