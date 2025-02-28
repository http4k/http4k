package org.http4k.security.oauth.metadata

import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.Moshi.auto
import org.http4k.routing.bind

fun WellKnownMetadata(metadata: ServerMetadata) = ".well-known/oauth-authorization-server" bind GET to {
    Response(OK).with(Body.auto<ServerMetadata>().toLens() of metadata)
}
