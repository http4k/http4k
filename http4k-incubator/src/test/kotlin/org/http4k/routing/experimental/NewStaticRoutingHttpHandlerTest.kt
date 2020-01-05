package org.http4k.routing.experimental

import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.StaticRoutingHttpHandlerTest
import org.http4k.routing.experimental.ResourceLoaders.Classpath

class NewStaticRoutingHttpHandlerTest : StaticRoutingHttpHandlerTest() {
    override val handler: RoutingHttpHandler = StaticRoutingHttpHandler(
        pathSegments = validPath,
        resourceLoader = Classpath()
    )
}
