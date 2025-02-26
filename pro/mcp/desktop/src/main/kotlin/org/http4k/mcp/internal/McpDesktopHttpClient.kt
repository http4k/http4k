package org.http4k.mcp.internal

import org.http4k.client.JavaHttpClient
import org.http4k.core.BodyMode
import org.http4k.core.Filter
import org.http4k.core.NoOp
import org.http4k.core.then
import org.http4k.filter.ClientFilters.Cookies
import org.http4k.filter.ClientFilters.FollowRedirects
import org.http4k.filter.DebuggingFilters.PrintRequestAndResponse
import org.http4k.filter.cookie.BasicCookieStorage
import org.http4k.mcp.McpOptions
import java.time.Clock

fun McpDesktopHttpClient(clock: Clock, mcpOptions: McpOptions) = with(mcpOptions) {
    val http = JavaHttpClient(responseBodyMode = BodyMode.Stream)

    val security = McpClientSecurity.from(mcpOptions, clock, http)

    (if (debug) PrintRequestAndResponse(System.err, true) else Filter.NoOp)
        .then(FollowRedirects())
        .then(Cookies(clock, BasicCookieStorage()))
        .then(security.filter)
        .then(http)
}
