package org.http4k.routing

class WebJarsHttpHandlerTest : RoutingHttpHandlerContract() {
    override val handler = webJars()

    override val validPath = "/webjars/swagger-ui/5.12.2/index.html"
}
