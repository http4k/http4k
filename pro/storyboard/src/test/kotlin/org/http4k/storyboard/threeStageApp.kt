/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.SpanKind
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.OpenTelemetryTracing
import org.http4k.filter.ServerFilters
import org.http4k.routing.bind
import org.http4k.routing.routes

/**
 * A small three-tier demo: webdriver -> server -> internal client -> server -> data.
 * Wraps both server and internal-client with OTel so the trace fans out into a tree
 * the HTTPExchangeConverter can render.
 */
internal fun threeStageApp(otel: OpenTelemetry): HttpHandler {
    lateinit var fullApp: HttpHandler
    val internal: HttpHandler = ClientFilters.OpenTelemetryTracing(otel).then { req -> fullApp(req) }

    val routes = routes(
        "/home" bind GET to { _ ->
            val data = internal(Request(GET, "http://localhost/data")).bodyString()
            Response(OK).body(
                """<!DOCTYPE html>
                |<html><head><title>Home</title></head>
                |<body>
                |  <h1>Welcome</h1>
                |  <p>Loaded from inner service: <strong>$data</strong></p>
                |  <a id="next" href="/detail">go deeper</a>
                |</body></html>""".trimMargin()
            )
        },
        "/detail" bind GET to { _ ->
            val data = internal(Request(GET, "http://localhost/data")).bodyString()
            Response(OK).body(
                """<!DOCTYPE html>
                |<html><head><title>Detail</title></head>
                |<body>
                |  <h1>Detail page</h1>
                |  <p>Detail data: <em>$data</em></p>
                |</body></html>""".trimMargin()
            )
        },
        "/data" bind GET to { _ ->
            otel.getTracer("db")
                .spanBuilder("SELECT greetings")
                .setSpanKind(SpanKind.CLIENT)
                .startSpan()
                .apply {
                    setAttribute("db.statement", "SELECT message FROM greetings WHERE id = 1")
                    setAttribute("db.system", "postgresql")
                    setAttribute("db.operation", "SELECT")
                    end()
                }
            Response(OK).body("hello inner")
        }
    )
    fullApp = ServerFilters.OpenTelemetryTracing(otel).then(routes)
    return fullApp
}
