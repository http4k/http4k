package org.http4k.hotreload

import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.orElse
import org.http4k.routing.poly

class ExamplePolyApp : HotReloadable<PolyHandler> {
    override fun create() = poly(orElse bind { req: Request -> Response(Status.OK).body("ss") })
}
