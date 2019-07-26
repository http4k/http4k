package org.http4k.routing

class SinglePageAppRoutingHttpHandlerTest : RoutingHttpHandlerContract() {
    override val handler: RoutingHttpHandler = SinglePageAppHandler(validPath,
        StaticRoutingHttpHandler(
            pathSegments = validPath,
            resourceLoader = ResourceLoader.Classpath(),
            extraFileExtensionToContentTypes = emptyMap()
        )
    )
}