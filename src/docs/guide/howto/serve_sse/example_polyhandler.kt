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
import org.http4k.sse.SseFilter
import org.http4k.sse.SseMessage
import org.http4k.sse.then
import kotlin.concurrent.thread

fun main() {
    val namePath = Path.of("name")

    // a filter allows us to intercept the call to the sse and do logging etc...
    val sayHello = SseFilter { next ->
        {
            println("Hello from the sse!")
            next(it)
        }
    }

    val sse = sayHello.then(
        sse(
            "/{name}" bind { sse: Sse ->
                val name = namePath(sse.connectRequest)
                thread {
                    repeat(10) {
                        sse.send(SseMessage.Data("hello $it"))
                        Thread.sleep(100)
                    }
                    sse.close()
                }
                sse.onClose { println("$name is closing") }
            }
        )
    )

    val http = { _: Request -> Response(OK).body("hiya world") }

    PolyHandler(http, sse = sse).asServer(Undertow(9000)).start()
}
