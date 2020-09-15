package org.http4k.google

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.body.form
import org.http4k.routing.RoutedRequest
import java.util.UUID

object GoogleAnalytics {
    operator fun invoke(analyticsHandler: HttpHandler,
                        trackingId: String,
                        clientId: (Request) -> String = { UUID.randomUUID().toString() }): Filter = object : Filter {

        override fun invoke(handler: HttpHandler): HttpHandler = { request ->
            handler(request).also {
                if (it.status.successful)
                    analyticsHandler(request.asPageView())
            }
        }

        private fun Request.asPageView(): Request {
            val host = header("host") ?: uri.host

            val path = when (this) {
                is RoutedRequest -> xUriTemplate.toString()
                else -> uri.path
            }

            return Request(POST, "/collect")
                .form(VERSION, "1")
                .form(MEASUREMENT_ID, trackingId)
                .form(CLIENT_ID, clientId(this))
                .form(DOCUMENT_TITLE, path)
                .form(DOCUMENT_PATH, path)
                .form(DOCUMENT_HOST, host)
        }
    }

    const val VERSION = "v"
    const val MEASUREMENT_ID = "tid"
    const val CLIENT_ID = "cid"
    const val DOCUMENT_TITLE = "dt"
    const val DOCUMENT_PATH = "dp"
    const val DOCUMENT_HOST = "dh"
}
