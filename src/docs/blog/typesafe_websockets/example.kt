package blog.typesafe_websockets

import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.format.Jackson.auto
import org.http4k.lens.Path
import org.http4k.routing.bind
import org.http4k.routing.websockets
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.http4k.websocket.PolyHandler
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsConsumer
import org.http4k.websocket.WsHandler
import org.http4k.websocket.WsMessage

// in json, this looks like: {"value": 123, "currency: "EUR" }
data class Money(val value: Int, val currency: String)

fun main(args: Array<String>) {

    // we use the Lens API to convert between the WsMessage and the Money instance, and to
    // dynamically bind the "name" from the path
    val moneyLens = WsMessage.auto<Money>().toLens()
    val nameLens = Path.of("name")

    // A WsConsumer is called when a websocket is connected and to attach event handlers to
    // - the websocket instance can be stored for future communication
    val consumer: WsConsumer = { ws: Websocket ->
        val name = nameLens(ws.upgradeRequest)
        ws.send(WsMessage("hello $name"))
        ws.onMessage {
            val received = moneyLens(it)
            ws.send(moneyLens(received))
        }
        ws.onClose { println("closed") }
    }

    // the routing API is virtually identical to the standard http4k http protocol routing API.
    val ws: WsHandler = websockets(
        "/hello" bind websockets(
            "/{name}" bind consumer
        )
    )

    val http: HttpHandler = { _: Request -> Response(OK).body("hiya world") }

    // the poly-handler can serve both http and ws protocols.
    PolyHandler(http, ws).asServer(Jetty(9000)).start().block()
}