package org.http4k.security.oauth.server

import org.http4k.routing.routes
import org.http4k.security.oauth.metadata.JsonWebKeySet
import org.http4k.security.oauth.metadata.ResourceMetadata

fun ResourceServerWellKnown(
    resource: ResourceMetadata,
    jsonWebKeySet: JsonWebKeySet? = null
) = routes(
    listOfNotNull(
        OAuthProtectedResourceMetadata(resource),
        jsonWebKeySet?.let(::OAuthJwks)
    )
)
