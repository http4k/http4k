package org.http4k.testing

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status

class HttpApp : HttpAppProvider {
    override fun invoke() = { req: Request -> Response(Status.OK).body("sss") }
}

