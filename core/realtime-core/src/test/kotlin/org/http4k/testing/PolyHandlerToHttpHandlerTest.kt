package org.http4k.testing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.routing.bindSse
import org.http4k.routing.poly
import org.http4k.routing.sse
import org.http4k.sse.SseMessage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.io.BufferedReader
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.concurrent.thread

class PolyHandlerToHttpHandlerTest {

    @Test
    fun `forwards a terminating SSE stream as the SSE wire format`() {
        val handler = poly("" bindSse sse {
            it.send(SseMessage.Event("first", "a"))
            it.send(SseMessage.Event("second", "b"))
            it.send(SseMessage.Event("third", "c"))
            it.close()
        }).toHttpHandler()

        val response = handler(Request(GET, "").header("Accept", "text/event-stream"))

        assertThat(
            response.bodyString(),
            equalTo(
                "event: first\ndata: a\n\n" +
                    "event: second\ndata: b\n\n" +
                    "event: third\ndata: c\n\n"
            )
        )
    }

    @Test
    @Timeout(value = 5, unit = SECONDS)
    fun `forwards a persistent SSE stream incrementally without deadlock`() {
        val secondEventReady = CountDownLatch(1)

        val handler = poly("" bindSse sse { sink ->
            sink.send(SseMessage.Event("first", "a"))
            thread(isDaemon = true, name = "test-second-event-emitter") {
                secondEventReady.await()
                sink.send(SseMessage.Event("second", "b"))
                sink.close()
            }
        }).toHttpHandler()

        val response = handler(Request(GET, "").header("Accept", "text/event-stream"))
        val reader = response.body.stream.bufferedReader(Charsets.UTF_8)

        assertThat(reader.readEvent(), equalTo(listOf("event: first", "data: a")))

        secondEventReady.countDown()

        assertThat(reader.readEvent(), equalTo(listOf("event: second", "data: b")))
    }

    private fun BufferedReader.readEvent(): List<String> {
        val lines = mutableListOf<String>()
        while (true) {
            val line = readLine() ?: break
            if (line.isEmpty()) break
            lines += line
        }
        return lines
    }
}
