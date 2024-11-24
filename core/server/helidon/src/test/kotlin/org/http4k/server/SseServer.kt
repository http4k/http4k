package org.http4k.server

import org.http4k.core.Request
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.routing.path
import org.http4k.routing.sse
import org.http4k.routing.sse.bind
import org.http4k.sse.SseMessage.Event
import org.http4k.sse.SseResponse

class SseServer {

    private var server: Http4kServer

    private val sse = sse("/hello" bind sse(
        "/{name}" bind { req: Request ->
            when {
                req.query("reject") == null -> SseResponse(ACCEPTED, listOf("foo" to "bar")) { sse ->
                    val name = req.path("name")!!
                    val message = Event("event2", "again $name\nHi!", "456")
                    sse.send(message)
                    sse.close()
                }

                else -> SseResponse { it.close() }
            }
        }
    )
    )

    init {
        server = PolyHandler(sse = sse).asServer(Helidon(8999)).start()
    }
}

fun main() {
    SseServer()
}
