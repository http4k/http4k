package org.http4k.security.oauth.metadata

import org.http4k.core.Uri
import org.http4k.security.ResponseType
import java.util.Locale

data class ServerMetadata(
    val issuer: String,
    val authorization_endpoint: Uri,
    val token_endpoint: Uri,
    val token_endpoint_auth_methods_supported: List<AuthMethod>,
    val token_endpoint_auth_signing_alg_values_supported: List<String>,
    val response_types_supported: List<ResponseType>,
    val scopes_supported: List<String> = emptyList(),
    val ui_locales_supported: List<Locale>? = null,
    val userinfo_endpoint: Uri? = null,
    val jwks_uri: Uri? = null,
    val registration_endpoint: Uri? = null,
    val service_documentation: Uri? = null,
    val signed_metadata: String? = null
)
