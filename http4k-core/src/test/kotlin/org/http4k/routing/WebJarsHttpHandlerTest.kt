package org.http4k.routing

class WebJarsHttpHandlerTest : RoutingHttpHandlerContract() {
    override val handler = webJars()

    override val validPath = "/webjars/swagger-ui/5.17.14/index.html"
}
