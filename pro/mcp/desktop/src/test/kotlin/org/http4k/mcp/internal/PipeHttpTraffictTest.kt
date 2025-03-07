package org.http4k.mcp.internal

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.contentType
import org.http4k.sse.SseMessage.Event
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.StringWriter

@Disabled
class PipeJsonRpcTrafficTest : PortBasedTest {

    @Test
    fun `pipes input and output to correct place`() {
        val inputMessages = listOf("hello", "world")
        val output = StringWriter()
        val sentToHttp = mutableListOf<String>()

        val expectedList = listOf(
            Event("message", "data1"),
            Event("message", "data2")
        )

        pipeJsonRpcTraffic(
            inputMessages.joinToString("\n").reader(),
            output,
            Request(POST, "http://host/sse"),
            { req: Request ->
                sentToHttp += req.bodyString()
                Response(OK)
                    .contentType(TEXT_EVENT_STREAM)
                    .body(expectedList.joinToString("\n") { it.toMessage() })
            }
        )

        assertThat(sentToHttp, equalTo(inputMessages))

        assertThat(
            output.toString().trimEnd().split("\n"),
            equalTo(listOf("hellodata1", "hellodata2", "worlddata1", "worlddata2"))
        )
    }
}
