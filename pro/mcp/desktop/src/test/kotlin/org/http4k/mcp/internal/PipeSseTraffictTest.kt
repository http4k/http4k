package org.http4k.mcp.internal

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.client.SseReconnectionMode.Immediate
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.contentType
import org.http4k.sse.SseMessage.Event
import org.junit.jupiter.api.Test
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.io.StringWriter
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

class PipeSseTrafficTest {

    @Test
    fun `pipes input and output to correct place`() {
        val inputMessages = listOf("hello", "world")
        val output = StringWriter()
        val sentToSse = mutableListOf<String>()
        val latch = CountDownLatch(2)

        val expectedList = listOf(
            Event("endpoint", "/bob"),
            Event("data1", "data1"),
            Event("data2", "data2")
        )
        val outputStream = PipedOutputStream()
        val inputStream = PipedInputStream(outputStream)

        val http = { req: Request ->
            when (req.uri.path) {
                "/sse" -> {
                    thread {
                        outputStream.use { o ->
                            expectedList.forEach { o.write(it.toMessage().toByteArray()) }
                            o.flush()
                        }
                    }

                    Response(OK)
                        .contentType(TEXT_EVENT_STREAM)
                        .body(inputStream)
                }

                "/bob" -> {
                    sentToSse.add(req.bodyString())
                    latch.countDown()
                    Response(ACCEPTED)
                }

                else -> error("Unexpected request to ${req.uri}")
            }
        }

        thread {
            pipeSseTraffic(
                inputMessages.joinToString("\n").reader(),
                output,
                Request(GET, "http://host/sse"),
                http,
                Immediate
            )
        }

        latch.await()

        assertThat(sentToSse, equalTo(inputMessages))

        assertThat(
            output.toString().trimEnd().split("\n"),
            equalTo(listOf("data1", "data2"))
        )
    }
}
