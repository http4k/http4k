package org.http4k.routing

open class WebJarsHttpHandlerTest : RoutingHttpHandlerContract() {
    override val handler: RoutingHttpHandler = webJars()

    override val validPath: String = "/webjars/swagger-ui/3.43.0/index.html"
}
