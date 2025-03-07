package org.http4k.mcp.internal

import org.http4k.client.JavaHttpClient
import org.http4k.core.BodyMode
import org.http4k.core.HttpHandler
import org.http4k.core.then
import org.http4k.filter.ClientFilters.Cookies
import org.http4k.filter.ClientFilters.FollowRedirects
import org.http4k.filter.cookie.BasicCookieStorage
import java.time.Clock

fun McpDesktopHttpClient(clock: Clock, security: McpClientSecurity): HttpHandler {
    val http = JavaHttpClient(responseBodyMode = BodyMode.Stream)

    return FollowRedirects()
        .then(Cookies(clock, BasicCookieStorage()))
        .then(security.filter)
        .then(http)
}
