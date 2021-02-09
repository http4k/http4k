package org.http4k.cloudevents

import io.cloudevents.CloudEvent
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.Router
import org.http4k.routing.RoutingHttpHandler
import org.http4k.server.SunHttp
import org.http4k.server.asServer

typealias CEHandler = (CloudEvent) -> Response

fun main() {
    cloudEvents(
        json(
            "" bind { req: CloudEvent -> Response(Status.OK) }
        ),
        csv(
            "" bind { req: CloudEvent -> Response(Status.OK) }
        )

    ).asServer(SunHttp()).start()
}

infix fun String.bind(ce: CEHandler) = this to ce

interface RoutingCEHandler : Router, CEHandler

fun json(vararg eventHandler: Pair<String, CEHandler>): RoutingCEHandler = TODO()
fun csv(vararg eventHandler: Pair<String, CEHandler>): RoutingCEHandler = TODO()
fun cloudEvents(vararg routingHttpHandler: RoutingCEHandler): RoutingHttpHandler = TODO()
