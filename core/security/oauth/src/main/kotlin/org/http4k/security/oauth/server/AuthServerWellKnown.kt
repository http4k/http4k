package org.http4k.security.oauth.server

import org.http4k.routing.routes
import org.http4k.security.oauth.metadata.JsonWebKeySet
import org.http4k.security.oauth.metadata.OAuthAuthorizationServerMetadata
import org.http4k.security.oauth.metadata.OAuthJwks
import org.http4k.security.oauth.metadata.OAuthOpenIdConfiguration
import org.http4k.security.oauth.metadata.OpenIdConfiguration
import org.http4k.security.oauth.metadata.ServerMetadata

fun AuthorizationServerWellKnown(
    server: ServerMetadata,
    jsonWebKeySet: JsonWebKeySet? = null,
    oidc: OpenIdConfiguration? = null
) = routes(
    listOfNotNull(
        OAuthAuthorizationServerMetadata(server),
        jsonWebKeySet?.let(::OAuthJwks),
        oidc?.let(::OAuthOpenIdConfiguration)
    )
)

