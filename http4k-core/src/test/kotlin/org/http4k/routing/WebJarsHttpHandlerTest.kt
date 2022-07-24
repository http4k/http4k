package org.http4k.routing

class WebJarsHttpHandlerTest : RoutingHttpHandlerContract() {
    override val handler: RoutingHttpHandler = webJars()

    override val validPath: String = "/webjars/swagger-ui/4.11.1/index.html"
}
