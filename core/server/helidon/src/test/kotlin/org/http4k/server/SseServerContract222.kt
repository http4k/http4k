package org.http4k.server

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.routing.sse
import org.http4k.routing.sse.bind
import org.http4k.sse.SseMessage.Data
import org.http4k.sse.SseMessage.Event
import org.http4k.sse.SseResponse
import org.http4k.routing.bind as hbind

class SseServerContract222() {

    var server: Http4kServer

    private val http = routes(
        "/hello/{name}" hbind { r: Request -> Response(OK).body(r.path("name")!!) }
    )

    private val sse = sse("/hello" bind sse(
        "/{name}" bind { req: Request ->
            when {
                req.query("reject") == null -> SseResponse(ACCEPTED, listOf("foo" to "bar")) { sse ->
                    val name = req.path("name")!!
                    sse.send(Event("event1", "hello $name", "123"))
                    sse.send(Event("event2", "again $name\nHi!", "456"))
                    sse.send(Data("goodbye $name".byteInputStream()))
                    sse.close()
                }

                else -> SseResponse { it.close() }
            }
        }
    )
    )

    init {
        server = PolyHandler(http, sse = sse).asServer(Helidon(8999)).start()
    }
}

fun main() {
    SseServerContract222()
}
