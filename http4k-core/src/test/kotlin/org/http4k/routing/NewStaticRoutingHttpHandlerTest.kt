package org.http4k.routing

import org.http4k.routing.experimental.NewResourceLoader
import org.http4k.routing.experimental.NewStaticRoutingHttpHandler

// I can't move this into experimental for some reason

class NewStaticRoutingHttpHandlerTest : StaticRoutingHttpHandlerTest() {
    override val handler: RoutingHttpHandler = NewStaticRoutingHttpHandler(
        pathSegments = validPath,
        resourceLoader = NewResourceLoader.Classpath()
    )
}
