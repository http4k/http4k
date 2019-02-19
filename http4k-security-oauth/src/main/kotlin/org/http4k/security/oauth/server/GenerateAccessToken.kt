package org.http4k.security.oauth.server

import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status

class GenerateAccessToken(private val accessTokens: AccessTokens) : HttpHandler {
        override fun invoke(request: Request): Response =
                Response(Status.OK).body(accessTokens.create().value)
}