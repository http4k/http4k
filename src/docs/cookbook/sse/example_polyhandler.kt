package cookbook.sse

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.Path
import org.http4k.routing.bind
import org.http4k.routing.sse
import org.http4k.server.PolyHandler
import org.http4k.server.Undertow
import org.http4k.server.asServer
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage

fun main() {
    val namePath = Path.of("name")

    val sse = sse(
        "/{name}" bind { sse: Sse ->
            val name = namePath(sse.connectRequest)
            sse.send(SseMessage.Data("hello"))
            sse.onClose { println("$name is closing") }
        }
    )
    val http = { _: Request -> Response(OK).body("hiya world") }

    PolyHandler(http, sse = sse).asServer(Undertow(9000)).start()
}
