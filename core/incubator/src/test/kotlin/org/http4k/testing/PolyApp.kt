package org.http4k.testing

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.server.PolyHandler

class PolyApp : PolyAppProvider {
    override fun invoke() = PolyHandler({ req: Request -> Response(Status.OK).body("sssssssssssss") })
}
