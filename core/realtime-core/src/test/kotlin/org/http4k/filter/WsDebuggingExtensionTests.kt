package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.testing.testWsClient
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsMessage.Mode.Binary
import org.http4k.websocket.WsMessage.Mode.Text
import org.http4k.websocket.WsResponse
import org.http4k.websocket.then
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class WsDebuggingExtensionTests {

    @Test
    fun `debug request`() {
        val os = ByteArrayOutputStream()
        val req = Request(POST, "").body("anything".byteInputStream())

        val socket = DebuggingFilters.PrintWsResponse(PrintStream(os))
            .then {
                WsResponse { ws ->
                    ws.send(WsMessage("hello", Text))
                    ws.send(WsMessage("hello", Binary))
                }
            }

        socket.testWsClient(req)

        val actual = String(os.toByteArray())
        assertThat(actual, containsSubstring("***** WS SEND POST:  -> Text: hello"))
        assertThat(actual, containsSubstring("***** WS SEND POST:  -> Binary: <<stream>>"))
    }

    @Test
    fun `debug response`() {
        val os = ByteArrayOutputStream()
        val req = Request(POST, "").body("anything".byteInputStream())

        val socket = DebuggingFilters.PrintWsResponse(PrintStream(os))
            .then {
                WsResponse { ws ->
                    ws.send(WsMessage("hello"))
                    ws.close()
                }
            }

        socket.testWsClient(req)

        val actual = String(os.toByteArray())
        assertThat(actual, containsSubstring("***** WS RESPONSE null to POST:  *****"))
        assertThat(actual, containsSubstring("***** WS SEND POST:  -> Text: hello"))
        assertThat(actual, containsSubstring("***** WS CLOSED with 1000 on POST:  *****"))
    }
}
