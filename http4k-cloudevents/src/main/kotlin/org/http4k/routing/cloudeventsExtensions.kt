package org.http4k.routing

import org.http4k.cloudevents.CEHandler
import org.http4k.cloudevents.toCloudEventReader
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.server.SunHttp
import org.http4k.server.asServer

infix fun PathMethod.to(cloudEvent: CEHandler): RoutingHttpHandler = TODO()

fun json(vararg eventHandler: CEHandler): RoutingCEHandler = TODO()
fun csv(vararg eventHandler: CEHandler): RoutingCEHandler = TODO()
fun cloudEvent(handler: CEHandler): RoutingHttpHandler = routes("" bind handler.toHttp())

fun CEHandler.toHttp(): HttpHandler = {
    it.toCloudEventReader().toEvent()
    this(it.toCloudEventReader().toEvent())
}

class RoutingHttpHandler1 : RoutingHttpHandler {
    override fun withFilter(new: Filter): RoutingHttpHandler {
        TODO("Not yet implemented")
    }

    override fun withBasePath(new: String): RoutingHttpHandler {
        TODO("Not yet implemented")
    }

    override fun match(request: Request): RouterMatch {
        TODO("Not yet implemented")
    }

    override fun invoke(p1: Request): Response {
        TODO("Not yet implemented")
    }

}

fun main() {
    routes(
        "/foo/bar" bind GET to cloudEvent(csv())
    ).asServer(SunHttp()).start()

//
//    cloudEvents(
//        json(
//            "" bind { req: CloudEvent -> Response(Status.OK) }
//        ),
//        csv(
//            "" bind { req: CloudEvent -> Response(Status.OK) }
//        )
//
//    ).asServer(SunHttp()).start()
}

interface RoutingCEHandler : Router, CEHandler

