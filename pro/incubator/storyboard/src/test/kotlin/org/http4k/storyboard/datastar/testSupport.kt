package org.http4k.storyboard.datastar

import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.sse.SseMessage

internal fun sseBody(vararg events: SseMessage.Event): String = events.joinToString("") { it.toMessage() }

internal fun driverFor(app: HttpHandler): DatastarWebDriver = DatastarWebDriver(app)

/** An app serving home at / and recording requests made to /probe. */
internal fun probeApp(home: String, probes: MutableList<Request>, vararg extraRoutes: RoutingHttpHandler): HttpHandler =
    routes(
        "/" bind GET to { Response(OK).body(home) },
        "/probe" bind GET to { probes.add(it); Response(OK).body("") },
        "/probe" bind POST to { probes.add(it); Response(OK).body("") },
        *extraRoutes
    )
