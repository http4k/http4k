/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.model

import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel

@JsonSerializable
@Polymorphic("type")
sealed class SecurityScheme {
    @JsonSerializable
    @PolymorphicLabel("apiKey")
    data class ApiKey(
        val name: String,
        val location: String,
        val description: String? = null
    ) : SecurityScheme()

    @JsonSerializable
    @PolymorphicLabel("http")
    data class HttpAuth(
        val scheme: String,
        val description: String? = null,
        val bearerFormat: String? = null
    ) : SecurityScheme()

    @JsonSerializable
    @PolymorphicLabel("oauth2")
    data class OAuth2(
        val flows: OAuthFlows,
        val description: String? = null,
        val oauth2MetadataUrl: Uri? = null
    ) : SecurityScheme()

    @JsonSerializable
    @PolymorphicLabel("openIdConnect")
    data class OpenIdConnect(
        val openIdConnectUrl: Uri,
        val description: String? = null
    ) : SecurityScheme()

    @JsonSerializable
    @PolymorphicLabel("mutualTls")
    data class MutualTls(
        val description: String? = null
    ) : SecurityScheme()
}

@JsonSerializable
@Polymorphic("type")
sealed class OAuthFlows {
    @JsonSerializable
    @PolymorphicLabel("authorizationCode")
    data class AuthorizationCode(
        val authorizationUrl: Uri,
        val tokenUrl: Uri,
        val scopes: Map<String, String>,
        val refreshUrl: Uri? = null,
        val pkceRequired: Boolean? = null
    ) : OAuthFlows()

    @JsonSerializable
    @PolymorphicLabel("clientCredentials")
    data class ClientCredentials(
        val tokenUrl: Uri,
        val scopes: Map<String, String>,
        val refreshUrl: Uri? = null
    ) : OAuthFlows()

    @JsonSerializable
    @PolymorphicLabel("deviceCode")
    data class DeviceCode(
        val deviceAuthorizationUrl: Uri,
        val tokenUrl: Uri,
        val scopes: Map<String, String>,
        val refreshUrl: Uri? = null
    ) : OAuthFlows()
}
