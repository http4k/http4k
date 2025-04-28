package org.http4k.security.oauth.metadata

import org.http4k.routing.routes

fun WellKnownMetadata(
    server: ServerMetadata,
    resource: ResourceMetadata? = null
) = routes(
    listOfNotNull(
        OAuthAuthorizationServerMetadata(server),
        resource?.let(::OAuthProtectedResourceMetadata)
    )
)
