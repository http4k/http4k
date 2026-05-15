/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.routing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.agui.AgUiError
import org.http4k.ai.agui.client.HttpAgUiClient
import org.http4k.ai.agui.event.AgUiEvent
import org.http4k.ai.agui.event.RunFinished
import org.http4k.ai.agui.event.RunStarted
import org.http4k.ai.agui.event.StateDelta
import org.http4k.ai.agui.event.TextMessageContent
import org.http4k.ai.agui.event.TextMessageEnd
import org.http4k.ai.agui.event.TextMessageStart
import org.http4k.ai.agui.event.ToolCallArgs
import org.http4k.ai.agui.event.ToolCallEnd
import org.http4k.ai.agui.event.ToolCallStart
import org.http4k.ai.agui.model.MessageId
import org.http4k.ai.agui.model.RunAgentInput
import org.http4k.ai.agui.model.RunId
import org.http4k.ai.agui.model.ThreadId
import org.http4k.ai.agui.model.ToolCallId
import org.http4k.ai.agui.util.AgUiJson
import org.http4k.ai.model.Role
import org.http4k.ai.model.ToolName
import org.http4k.core.Uri
import org.junit.jupiter.api.Test

/**
 * End-to-end conformance test: client → SDK round-trip over an in-memory HttpHandler,
 * exercising a representative slice of the AG-UI lifecycle.
 */
class AgUiClientServerTest {

    @Test
    fun `client streams the full event sequence emitted by the server`() {
        val threadId = ThreadId.of("thread-conformance")
        val runId = RunId.of("run-conformance")
        val messageId = MessageId.of("msg-1")
        val toolCallId = ToolCallId.of("tool-1")

        val expected: List<AgUiEvent> = listOf(
            RunStarted(threadId, runId),
            TextMessageStart(messageId, Role.Assistant),
            TextMessageContent(messageId, "Hello "),
            TextMessageContent(messageId, "world"),
            TextMessageEnd(messageId),
            ToolCallStart(toolCallId, ToolName.of("search")),
            ToolCallArgs(toolCallId, """{"q":"http4k"}"""),
            ToolCallEnd(toolCallId),
            StateDelta(listOf(AgUiJson.parse("""{"op":"replace","path":"/status","value":"done"}"""))),
            RunFinished(threadId, runId, result = AgUiJson.parse("\"ok\""))
        )

        val server = agUi { _ -> expected.asSequence() }
        val client = HttpAgUiClient(Uri.of("/"),server)

        val result = client(RunAgentInput(threadId, runId))

        assertThat(result is Success, equalTo(true))
        assertThat(result.valueOrNull()!!.toList(), equalTo(expected))
    }

    @Test
    fun `client surfaces non-2xx responses as AgUiError Http`() {
        val emptyServer = agUi(basePath = "/agent") { _ -> emptySequence() }
        val client = HttpAgUiClient(Uri.of("/"),emptyServer)

        val result = client(RunAgentInput(ThreadId.of("t"), RunId.of("r")))

        assertThat(result is Failure, equalTo(true))
        val error = (result as Failure).reason
        assertThat(error is AgUiError.Http, equalTo(true))
        assertThat((error as AgUiError.Http).response.status.code, equalTo(404))
    }
}
