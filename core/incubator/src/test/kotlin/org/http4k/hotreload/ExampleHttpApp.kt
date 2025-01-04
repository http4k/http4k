package org.http4k.hotreload

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status

class ExampleHttpApp : HotReloadable.Http {
    override fun create() = { req: Request -> Response(Status.OK).body("asdsassd") }
}
