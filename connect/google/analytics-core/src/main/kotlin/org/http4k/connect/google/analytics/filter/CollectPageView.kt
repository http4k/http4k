package org.http4k.connect.google.analytics.filter

import org.http4k.connect.google.analytics.model.AnalyticsCollector
import org.http4k.connect.google.analytics.model.ClientId
import org.http4k.connect.google.analytics.model.PageView
import org.http4k.connect.google.analytics.model.UserAgent
import org.http4k.core.Filter
import org.http4k.core.Request
import org.http4k.routing.RoutedRequest
import java.util.UUID

fun CollectPageView(
    collector: AnalyticsCollector,
    clientId: (Request) -> ClientId = { ClientId.of(UUID.randomUUID().toString()) }
): Filter = Filter { handler ->
    { request ->
        handler(request).also {
            if (it.status.successful || it.status.informational || it.status.redirection) {
                val host = request.header("host") ?: request.uri.host
                val path = when (request) {
                    is RoutedRequest -> request.xUriTemplate.toString()
                    else -> request.uri.path
                }
                val userAgent = it.header("User-Agent")?.let(UserAgent::of) ?: UserAgent.Default
                collector(PageView(path, path, host, clientId(request), userAgent))
            }
        }
    }
}

