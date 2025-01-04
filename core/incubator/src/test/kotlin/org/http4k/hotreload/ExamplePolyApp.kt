package org.http4k.hotreload

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.server.PolyHandler

class ExamplePolyApp : HotReloadable.Poly {
    override fun create() = PolyHandler({ req: Request -> Response(Status.OK).body("ss") })
}
