package org.http4k.security.oauth.metadata

import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.security.oauth.format.OAuthMoshi.json

fun OAuthAuthorizationServerMetadata(server: ServerMetadata) =
    ".well-known/oauth-authorization-server" bind GET to { Response(OK).json(server) }
