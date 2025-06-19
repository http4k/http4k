package org.http4k.security.oauth.metadata

import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.security.oauth.format.OAuthMoshi.json

fun OAuthOpenIdConfiguration(configuration: OpenIdConfiguration) =
    ".well-known/openid-configuration" bind Method.GET to { Response(OK).json(configuration) }
