/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.routing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.agui.event.AgUiEvent
import org.http4k.ai.agui.event.RunFinished
import org.http4k.ai.agui.event.RunStarted
import org.http4k.ai.agui.event.TextMessageChunk
import org.http4k.ai.agui.model.MessageId
import org.http4k.ai.agui.model.RunAgentInput
import org.http4k.ai.agui.model.RunId
import org.http4k.ai.agui.model.ThreadId
import org.http4k.ai.agui.util.AgUiJson
import org.http4k.ai.agui.util.AgUiJson.auto
import org.http4k.ai.model.Role
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.METHOD_NOT_ALLOWED
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.sse.SseMessage
import org.http4k.sse.chunkedSseSequence
import org.junit.jupiter.api.Test

class AgUiTest {

    private val input = RunAgentInput(
        threadId = ThreadId.of("t-1"),
        runId = RunId.of("r-1")
    )

    private val runAgentInputLens = Body.auto<RunAgentInput>().toLens()

    @Test
    fun `POST streams handler events as SSE with text-event-stream content type`() {
        val server = agUi { req ->
            sequenceOf(
                RunStarted(req.threadId, req.runId),
                TextMessageChunk(messageId = MessageId.of("m-1"), role = Role.Assistant, delta = "Hello"),
                RunFinished(req.threadId, req.runId)
            )
        }

        val response = server(Request(POST, "/").with(runAgentInputLens of input))

        assertThat(response.status, equalTo(OK))
        assertThat(response.header("content-type")!!, containsSubstring(ContentType.TEXT_EVENT_STREAM.value))

        val events = response.body.stream.chunkedSseSequence()
            .filterIsInstance<SseMessage.Data>()
            .map { AgUiJson.asA<AgUiEvent>(it.data) }
            .toList()

        assertThat(events.size, equalTo(3))
        assertThat(events[0] is RunStarted, equalTo(true))
        assertThat((events[1] as TextMessageChunk).delta, equalTo("Hello"))
        assertThat(events[2] is RunFinished, equalTo(true))
    }

    @Test
    fun `lazy handler is consumed lazily — events emitted in order`() {
        val emitted = mutableListOf<String>()
        val server = agUi { _ ->
            sequence {
                emitted.add("a")
                yield(RunStarted(ThreadId.of("t"), RunId.of("r")))
                emitted.add("b")
                yield(RunFinished(ThreadId.of("t"), RunId.of("r")))
                emitted.add("c")
            }
        }

        val response = server(Request(POST, "/").with(runAgentInputLens of input))
        response.body.stream.chunkedSseSequence().filterIsInstance<SseMessage.Data>().toList()

        assertThat(emitted, equalTo(listOf("a", "b", "c")))
    }

    @Test
    fun `non-POST returns 405`() {
        val server = agUi { _ -> emptySequence() }
        val response = server(Request(GET, "/"))
        assertThat(response.status, equalTo(METHOD_NOT_ALLOWED))
    }

    @Test
    fun `custom basePath is bound`() {
        val server = agUi(basePath = "/agent") { _ ->
            sequenceOf(RunStarted(ThreadId.of("t"), RunId.of("r")))
        }

        val response = server(Request(POST, "/agent").with(runAgentInputLens of input))
        assertThat(response.status, equalTo(OK))
    }
}
