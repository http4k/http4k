package org.http4k.routing

class SinglePageAppRoutingHttpHandlerTest : RoutingHttpHandlerContract() {
    override val handler: RoutingHttpHandler = singlePageApp()
}