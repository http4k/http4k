package org.http4k.routing

import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.Path
import org.http4k.lens.int

fun bob(i: Int): HttpHandler = { Response(OK).body("bob") }
fun bob2(i: Int, s: String): HttpHandler = { Response(OK).body("bob2") }
fun bob2(i: Int, s: Int): HttpHandler = { Response(OK).body("bob2") }

fun main(args: Array<String>) {

    val app = routes(
        "/contract" by (contract())(
            GET to "value" / Path.int().of("world") bindTo ::bob describedBy Desc(),
            GET to Path.int().of("world") bindTo ::bob,
            GET to Path.int().of("world1") / Path.int().of("world2") bindTo ::bob2,
            GET to "value" / Path.int().of("world") bindTo ::bob,
            GET to "value" / Path.int().of("world") / "asd" bindTo ::bob2,
            GET to "/" bindTo { _: Request -> Response(OK) } describedBy Desc()
        )
    )

    println(app(Request(GET, "/contract")))
    println(app(Request(GET, "/contract/123")))
    println(app(Request(GET, "/contract/value/123")))
    println(app(Request(GET, "/contract/value/123/asd")))
}
