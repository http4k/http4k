package org.http4k.routing

import org.http4k.routing.experimental.ResourceLoaders
import org.http4k.routing.experimental.StaticRoutingHttpHandler

// I can't move this into experimental for some reason

class NewStaticRoutingHttpHandlerTest : StaticRoutingHttpHandlerTest() {
    override val handler: RoutingHttpHandler = StaticRoutingHttpHandler(
        pathSegments = validPath,
        resourceLoader = ResourceLoaders.Classpath()
    )
}
