package cookbook.websockets

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.testing.testWsClient
import org.http4k.websocket.WsMessage
import org.junit.Test

fun main(args: Array<String>) {
    val client = app.testWsClient(Request(Method.GET, "ws://localhost:9000/hello/same"))!!

    client.send(WsMessage("1"))
    client.send(WsMessage("2"))
    client.close(Status(200, "bob"))

    client.received.take(2).forEach {
        println("received back: " + body(it))
    }
}

class WebsocketAppTest {
    @Test
    fun `can `() {
        assertThat(false, equalTo(false))
    }
    
}