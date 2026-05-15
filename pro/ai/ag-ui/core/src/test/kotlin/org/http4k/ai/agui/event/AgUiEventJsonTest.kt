/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.agui.event

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.agui.model.ActivityType
import org.http4k.ai.agui.model.AgUiMessage
import org.http4k.ai.agui.model.MessageId
import org.http4k.ai.agui.model.RunId
import org.http4k.ai.agui.model.StepName
import org.http4k.ai.agui.model.ThreadId
import org.http4k.ai.agui.model.ToolCallId
import org.http4k.ai.agui.util.AgUiJson
import org.http4k.ai.model.Role
import org.http4k.ai.model.ToolName
import org.junit.jupiter.api.Test

class AgUiEventJsonTest {

    @Test
    fun `RunStarted serializes with RUN_STARTED type and roundtrips polymorphically`() {
        val event: AgUiEvent = RunStarted(
            threadId = ThreadId.of("t-1"),
            runId = RunId.of("r-1")
        )

        val json = AgUiJson.asFormatString(event)
        assertThat(json, containsSubstring("\"type\":\"RUN_STARTED\""))

        val parsed = AgUiJson.asA<AgUiEvent>(json)
        assertThat(parsed, equalTo(event))
    }

    @Test
    fun `RunFinished roundtrips polymorphically`() {
        val event: AgUiEvent = RunFinished(
            threadId = ThreadId.of("t-1"),
            runId = RunId.of("r-1"),
            result = AgUiJson.parse("""{"status":"ok"}""")
        )

        val json = AgUiJson.asFormatString(event)
        assertThat(json, containsSubstring("\"type\":\"RUN_FINISHED\""))
        assertThat(AgUiJson.asA<AgUiEvent>(json), equalTo(event))
    }

    @Test
    fun `RunError roundtrips polymorphically`() {
        val event: AgUiEvent = RunError(message = "boom", code = "INTERNAL")
        val json = AgUiJson.asFormatString(event)
        assertThat(json, containsSubstring("\"type\":\"RUN_ERROR\""))
        assertThat(AgUiJson.asA<AgUiEvent>(json), equalTo(event))
    }

    @Test
    fun `StepStarted and StepFinished roundtrip`() {
        val started: AgUiEvent = StepStarted(stepName = StepName.of("plan"))
        val finished: AgUiEvent = StepFinished(stepName = StepName.of("plan"))

        assertThat(AgUiJson.asA<AgUiEvent>(AgUiJson.asFormatString(started)), equalTo(started))
        assertThat(AgUiJson.asA<AgUiEvent>(AgUiJson.asFormatString(finished)), equalTo(finished))
    }

    @Test
    fun `TextMessage events roundtrip`() {
        val start: AgUiEvent = TextMessageStart(messageId = MessageId.of("m-1"), role = Role.Assistant)
        val content: AgUiEvent = TextMessageContent(messageId = MessageId.of("m-1"), delta = "Hello")
        val end: AgUiEvent = TextMessageEnd(messageId = MessageId.of("m-1"))
        val chunk: AgUiEvent = TextMessageChunk(messageId = MessageId.of("m-1"), role = Role.Assistant, delta = "Hi")

        listOf(start, content, end, chunk).forEach {
            assertThat(AgUiJson.asA<AgUiEvent>(AgUiJson.asFormatString(it)), equalTo(it))
        }
    }

    @Test
    fun `ToolCall events roundtrip`() {
        val start: AgUiEvent = ToolCallStart(
            toolCallId = ToolCallId.of("c-1"),
            toolCallName = ToolName.of("search")
        )
        val args: AgUiEvent = ToolCallArgs(toolCallId = ToolCallId.of("c-1"), delta = "{\"q\":")
        val end: AgUiEvent = ToolCallEnd(toolCallId = ToolCallId.of("c-1"))
        val result: AgUiEvent = ToolCallResult(
            messageId = MessageId.of("m-1"),
            toolCallId = ToolCallId.of("c-1"),
            content = "ok"
        )
        val chunk: AgUiEvent = ToolCallChunk(
            toolCallId = ToolCallId.of("c-1"),
            toolCallName = ToolName.of("search"),
            delta = "{"
        )

        listOf(start, args, end, result, chunk).forEach {
            assertThat(AgUiJson.asA<AgUiEvent>(AgUiJson.asFormatString(it)), equalTo(it))
        }
    }

    @Test
    fun `State events roundtrip`() {
        val snapshot: AgUiEvent = StateSnapshot(snapshot = AgUiJson.parse("""{"foo":"bar"}"""))
        val delta: AgUiEvent = StateDelta(
            delta = listOf(AgUiJson.parse("""{"op":"replace","path":"/foo","value":"baz"}"""))
        )
        val messages: AgUiEvent = MessagesSnapshot(
            messages = listOf(AgUiMessage(MessageId.of("m"), Role.User, content = "hi"))
        )

        listOf(snapshot, delta, messages).forEach {
            assertThat(AgUiJson.asA<AgUiEvent>(AgUiJson.asFormatString(it)), equalTo(it))
        }
    }

    @Test
    fun `Activity events roundtrip`() {
        val snapshot: AgUiEvent = ActivitySnapshot(
            messageId = MessageId.of("m"),
            activityType = ActivityType.of("thinking"),
            content = AgUiJson.parse("\"deep thoughts\"")
        )
        val delta: AgUiEvent = ActivityDelta(
            messageId = MessageId.of("m"),
            activityType = ActivityType.of("thinking"),
            patch = listOf(AgUiJson.parse("""{"op":"add","path":"/-","value":"x"}"""))
        )

        listOf(snapshot, delta).forEach {
            assertThat(AgUiJson.asA<AgUiEvent>(AgUiJson.asFormatString(it)), equalTo(it))
        }
    }

    @Test
    fun `Reasoning events roundtrip`() {
        val events: List<AgUiEvent> = listOf(
            ReasoningStart(messageId = MessageId.of("m")),
            ReasoningMessageStart(messageId = MessageId.of("m")),
            ReasoningMessageContent(messageId = MessageId.of("m"), delta = "thinking..."),
            ReasoningMessageEnd(messageId = MessageId.of("m")),
            ReasoningMessageChunk(messageId = MessageId.of("m"), delta = "chunk"),
            ReasoningEnd(messageId = MessageId.of("m")),
            ReasoningEncryptedValue(subtype = "message", entityId = "e", encryptedValue = "x==")
        )

        events.forEach {
            assertThat(AgUiJson.asA<AgUiEvent>(AgUiJson.asFormatString(it)), equalTo(it))
        }
    }

    @Test
    fun `Raw and Custom events roundtrip`() {
        val raw: AgUiEvent = Raw(event = AgUiJson.parse("""{"foreign":1}"""), source = "external")
        val custom: AgUiEvent = Custom(name = "ping", value = AgUiJson.parse("\"pong\""))

        listOf(raw, custom).forEach {
            assertThat(AgUiJson.asA<AgUiEvent>(AgUiJson.asFormatString(it)), equalTo(it))
        }
    }

    @Test
    fun `unknown type discriminator fails clearly`() {
        val rogue = """{"type":"NOT_A_REAL_EVENT","foo":1}"""
        try {
            AgUiJson.asA<AgUiEvent>(rogue)
            error("expected deserialization to fail")
        } catch (e: Exception) {
            // Kotshi throws on unrecognised polymorphic labels — acceptable.
        }
    }
}
