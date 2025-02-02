package org.http4k.mcp.internal

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.time.DeterministicScheduler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Uri
import org.http4k.sse.SseClient
import org.http4k.sse.SseMessage.Event
import org.junit.jupiter.api.Test
import java.io.StringWriter

class PipeSseTraffictTest {

    @Test
    fun `pipes input and output to correct place`() {

        val messages = listOf("hello", "world")

        val output = StringWriter()
        val sentToSse = mutableListOf<String>()

        val expectedList = listOf(
            Event("endpoint", "/bob"),
            Event("data1", "data1"),
            Event("data2", "data2")
        )

        val scheduler = DeterministicScheduler()

        pipeSseTraffic(
            messages.joinToString("\n").reader(),
            output,
            scheduler,
            Request(GET, "http://host/sse"),
            {
                assertThat(it.uri, equalTo(Uri.of("http://host/bob")))
                sentToSse.add(it.bodyString())
                Response(ACCEPTED)
            },
            { _: Request ->
                object : SseClient {
                    override fun received() = expectedList.asSequence()
                    override fun close() {}
                }
            }
        )

        scheduler.runUntilIdle()

        assertThat(sentToSse, equalTo(messages))

        assertThat(output.buffer.split("\n"), equalTo(listOf("data1", "data2", "")))
    }
}

