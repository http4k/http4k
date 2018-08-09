package org.http4k.routing

class NewStaticRoutingHttpHandlerTest : StaticRoutingHttpHandlerTest() {
    override val handler: RoutingHttpHandler = NewStaticRoutingHttpHandler(
        pathSegments = validPath,
        resourceLoader = NewResourceLoader.Classpath(),
        extraPairs = emptyMap()
    )
}
