package org.http4k

import org.http4k.contract.NoRenderer
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.Path
import org.http4k.lens.int
import org.http4k.routing.routes


fun bob(i: Int): HttpHandler = TODO()

fun main(args: Array<String>) {

    val app = routes(
        "/contract" by cont(NoRenderer)(
            GET to "/" bindTo { request: Request -> Response(OK) } describedBy Desc(),
            GET to "value" / Path.int().of("world") bindTo ::bob describedBy Desc(),
            GET to "value2" / Path.int().of("world") bindTo ::bob
        )
    )

    app(Request(GET, "/contract/value2/bob/bob2"))
}
