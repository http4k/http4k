package org.http4k.security.oauth.server

import org.http4k.routing.routes
import org.http4k.security.oauth.metadata.OAuthProtectedResourceMetadata
import org.http4k.security.oauth.metadata.ResourceMetadata

fun ResourceServerWellKnown(resource: ResourceMetadata) = routes(
    OAuthProtectedResourceMetadata(resource)
)
