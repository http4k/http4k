package org.http4k.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.accept
import org.http4k.routing.sse
import org.http4k.server.Helidon
import org.http4k.server.asServer
import org.http4k.sse.SseMessage.Data
import org.http4k.sse.SseMessage.Event
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Test

class JavaSseClientTest : PortBasedTest {

    @Test
    fun `gets all messages`() {
        val server = sse {
            it.send(Event("event", "data1"))
            it.send(Data("data2"))
            it.close()
        }.asServer(Helidon(0)).start()

        try {
            val responses = JavaSseClient()(
                Request(GET, "http://localhost:${server.port()}").accept(TEXT_EVENT_STREAM)
            ).received()

            assertThat(
                responses.toList(), equalTo(
                    listOf(
                        Event("event", "data1"),
                        Data("data2")
                    )
                )
            )
        } finally {
            server.stop()
        }
    }

    @Test
    fun `blows up on illegal message`() {
        val server = { _: Request -> Response(OK).body("foobar\n\n") }
            .asServer(Helidon(0)).start()
        try {
            assertThat(
                {
                    JavaSseClient()(
                        Request(GET, "http://localhost:${server.port()}")
                            .accept(TEXT_EVENT_STREAM)
                    ).received().toList()
                },
                throws<IllegalArgumentException>()
            )
        } finally {
            server.stop()
        }
    }
}
