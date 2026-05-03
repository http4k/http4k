package org.http4k.ai.a2a.model

import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

sealed class OAuthFlows {
    @JsonSerializable
    data class AuthorizationCode(
        val authorizationUrl: Uri,
        val tokenUrl: Uri,
        val scopes: Map<String, String>,
        val refreshUrl: Uri? = null,
        val pkceRequired: Boolean? = null
    ) : OAuthFlows()

    @JsonSerializable
    data class ClientCredentials(
        val tokenUrl: Uri,
        val scopes: Map<String, String>,
        val refreshUrl: Uri? = null
    ) : OAuthFlows()

    @JsonSerializable
    data class DeviceCode(
        val deviceAuthorizationUrl: Uri,
        val tokenUrl: Uri,
        val scopes: Map<String, String>,
        val refreshUrl: Uri? = null
    ) : OAuthFlows()
}
