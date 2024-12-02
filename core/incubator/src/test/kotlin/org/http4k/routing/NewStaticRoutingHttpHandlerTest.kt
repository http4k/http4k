package org.http4k.routing

import org.http4k.routing.experimental.ResourceLoaders.Classpath

class NewStaticRoutingHttpHandlerTest : StaticRoutingHttpHandlerTest() {
    override val handler = validPath bind org.http4k.routing.experimental.static(
        resourceLoader = Classpath()
    )
}
