package org.http4k.mcp.internal

import org.http4k.client.JavaHttpClient
import org.http4k.core.BodyMode
import org.http4k.core.Filter
import org.http4k.core.NoOp
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.DebuggingFilters
import org.http4k.filter.cookie.BasicCookieStorage
import java.time.Clock

fun McpDesktopHttpClient(clock: Clock, debug: Boolean) =
    (if (debug) DebuggingFilters.PrintRequestAndResponse(System.err, true) else Filter.NoOp)
        .then(ClientFilters.FollowRedirects())
        .then(ClientFilters.Cookies(clock, BasicCookieStorage()))
        .then(JavaHttpClient(responseBodyMode = BodyMode.Stream))
