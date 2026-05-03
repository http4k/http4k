/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.model

import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

sealed class SecurityScheme {
    abstract val description: String?

    @JsonSerializable
    data class ApiKey(
        val name: String,
        val location: String,
        override val description: String? = null
    ) : SecurityScheme()

    @JsonSerializable
    data class HttpAuth(
        val scheme: String,
        override val description: String? = null,
        val bearerFormat: String? = null
    ) : SecurityScheme()

    @JsonSerializable
    data class OAuth2(
        val flows: OAuthFlows,
        override val description: String? = null,
        val oauth2MetadataUrl: Uri? = null
    ) : SecurityScheme()

    @JsonSerializable
    data class OpenIdConnect(
        val openIdConnectUrl: Uri,
        override val description: String? = null
    ) : SecurityScheme()

    @JsonSerializable
    data class MutualTls(
        override val description: String? = null
    ) : SecurityScheme()
}

