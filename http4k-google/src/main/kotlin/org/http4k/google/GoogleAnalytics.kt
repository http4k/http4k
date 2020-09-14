package org.http4k.google

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.body.form
import org.http4k.routing.RoutedRequest
import java.util.UUID

class GoogleAnalytics(private val clientHandler: HttpHandler,
                      private val trackingId: String,
                      private val clientId: (Request) -> String = { UUID.randomUUID().toString() }) : Filter {

    override fun invoke(handler: HttpHandler): HttpHandler = { request ->
        handler(request).let {
            val response = clientHandler(request.asPageView())
            when {
                response.status.successful -> it
                else -> response
            }
        }
    }

    private fun Request.asPageView(): Request {
        val host = header("host") ?: ""

        val path = when (this) {
            is RoutedRequest -> xUriTemplate.toString()
            else -> uri.path
        }

        return Request(POST, "/collect")
            .form("v", "1")
            .form("tid", trackingId)
            .form("cid", clientId(this))
            .form("dt", path)
            .form("dp", path)
            .form("dh", host)
    }
}
