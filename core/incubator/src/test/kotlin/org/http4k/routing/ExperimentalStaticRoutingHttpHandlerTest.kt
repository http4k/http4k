package org.http4k.routing

import org.http4k.routing.experimental.ResourceLoaders.Classpath
import org.junit.jupiter.api.Disabled

@Disabled
class ExperimentalStaticRoutingHttpHandlerTest : StaticRoutingHttpHandlerTest() {
    override val handler = validPath bind org.http4k.routing.experimental.static(
        resourceLoader = Classpath()
    )
}
