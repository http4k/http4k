package cookbook.websockets

import org.http4k.client.WebsocketClient
import org.http4k.core.Uri
import org.http4k.format.Jackson.auto
import org.http4k.routing.bind
import org.http4k.routing.websockets
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsMessage

data class Person(val name: String, val age: Int)

fun main() {

    // a lens that will marshall the Person object on and off the wire
    val personLens = WsMessage.auto<Person>().toLens()

    val server = websockets(
        "/ageMe" bind { ws: Websocket ->
            ws.onMessage {
                val person = personLens(it)
                ws.send(personLens.create(person.copy(age = person.age + 10)))
                ws.close()
            }
        }
    ).asServer(Jetty(8000)).start()

    val client = WebsocketClient.blocking(Uri.of("ws://localhost:8000/ageMe"))

    // send a message in "native form" - we could also use the Lens here to auto-marshall
    client.send(WsMessage("""{ "name":"bob", "age": 25 }"""))

    // read all of the messages from the socket until it is closed (by the server).
    // we expect to get one message back before the stream is closed.
    client.received().toList().forEach(::println)

    server.stop()
}
