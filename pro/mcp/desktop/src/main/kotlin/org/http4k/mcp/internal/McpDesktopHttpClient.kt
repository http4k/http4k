package org.http4k.mcp.internal

import org.http4k.client.JavaHttpClient
import org.http4k.core.BodyMode
import org.http4k.core.Filter
import org.http4k.core.NoOp
import org.http4k.core.then
import org.http4k.filter.ClientFilters.ApiKeyAuth
import org.http4k.filter.ClientFilters.BasicAuth
import org.http4k.filter.ClientFilters.BearerAuth
import org.http4k.filter.ClientFilters.Cookies
import org.http4k.filter.ClientFilters.FollowRedirects
import org.http4k.filter.DebuggingFilters.PrintRequestAndResponse
import org.http4k.filter.cookie.BasicCookieStorage
import org.http4k.lens.Header
import org.http4k.mcp.McpOptions
import java.time.Clock

fun McpDesktopHttpClient(clock: Clock, mcpOptions: McpOptions) = with(mcpOptions) {
    val security = when {
        apiKey != null -> ApiKeyAuth(Header.required(apiKeyHeader) of apiKey!!)
        basicAuth != null -> BasicAuth(basicAuth!!.substringBefore(":"), basicAuth!!.substringAfterLast(":"))
        bearerToken != null -> BearerAuth(bearerToken!!)
        else -> Filter.NoOp
    }

    (if (debug) PrintRequestAndResponse(System.err, true) else Filter.NoOp)
        .then(FollowRedirects())
        .then(Cookies(clock, BasicCookieStorage()))
        .then(security)
        .then(JavaHttpClient(responseBodyMode = BodyMode.Stream))
}
