package org.http4k.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.BodyMode.Stream
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.accept
import org.http4k.routing.sse
import org.http4k.server.Helidon
import org.http4k.server.asServer
import org.http4k.sse.SseMessage
import org.http4k.sse.SseMessage.Data
import org.http4k.sse.SseMessage.Event
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class Http4kSseClientTest : PortBasedTest {

    @Test
    fun `gets all messages`() {
        val server = sse {
            it.send(Event("event", "data1"))
            it.send(Data("data2"))
            it.close()
        }.asServer(Helidon(0)).start()

        try {
            val messages = LinkedBlockingQueue<SseMessage>()

            thread {
                Http4kSseClient(
                    Request(GET, "http://localhost:${server.port()}").accept(TEXT_EVENT_STREAM),
                    JavaHttpClient(responseBodyMode = Stream)
                )
                    .received()
                    .forEach { msg ->
                        messages.add(msg)
                    }
            }

            assertThat(messages.take(), equalTo(Event("event", "data1")))
            assertThat(messages.take(), equalTo(Data("data2")))

        } finally {
            server.stop()
        }
    }

    @Test
    fun `ignores illegal messages`() {
        val goodMessage = Event("foo", "bar")
        val server = { _: Request -> Response(OK).body("foobar\n\n" + goodMessage.toMessage()) }
            .asServer(Helidon(0))
            .start()

        try {
            val okMessage = AtomicReference<SseMessage>()
            val latch = CountDownLatch(1)

            thread {
                Http4kSseClient(
                    Request(GET, "http://localhost:${server.port()}").accept(TEXT_EVENT_STREAM),
                    JavaHttpClient(responseBodyMode = Stream)
                )
                    .received()
                    .forEach { msg ->
                        okMessage.set(msg)
                        latch.countDown()
                    }
            }

            latch.await()
            assertThat(okMessage.get(), equalTo(goodMessage))
        } finally {
            server.stop()
        }
    }
}
