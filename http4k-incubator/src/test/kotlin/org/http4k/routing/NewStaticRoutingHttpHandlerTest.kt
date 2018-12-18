package org.http4k.routing

import org.http4k.routing.experimental.ResourceLoaders.Classpath
import org.http4k.routing.experimental.StaticRoutingHttpHandler

class NewStaticRoutingHttpHandlerTest : StaticRoutingHttpHandlerTest() {
    override val handler: RoutingHttpHandler = StaticRoutingHttpHandler(
        pathSegments = validPath,
        resourceLoader = Classpath()
    )
}
