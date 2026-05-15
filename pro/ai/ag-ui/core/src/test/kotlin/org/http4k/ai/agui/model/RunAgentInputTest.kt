/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.agui.model

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.agui.util.AgUiJson
import org.http4k.ai.model.Role
import org.http4k.ai.model.ToolName
import org.http4k.format.MoshiObject
import org.junit.jupiter.api.Test

class RunAgentInputTest {

    @Test
    fun `RunAgentInput roundtrips through JSON with all fields`() {
        val input = RunAgentInput(
            threadId = ThreadId.of("thread-1"),
            runId = RunId.of("run-1"),
            messages = listOf(
                AgUiMessage(
                    id = MessageId.of("msg-1"),
                    role = Role.User,
                    content = "Hello"
                )
            ),
            tools = listOf(
                AgUiTool(
                    name = ToolName.of("search"),
                    description = "Searches the web",
                    parameters = AgUiJson.parse("""{"type":"object","properties":{}}""")
                )
            ),
            context = listOf(Context("Locale", "en-GB"))
        )

        val json = AgUiJson.asFormatString(input)
        assertThat(AgUiJson.asA<RunAgentInput>(json), equalTo(input))
    }

    @Test
    fun `RunAgentInput uses camelCase JSON keys`() {
        val input = RunAgentInput(
            threadId = ThreadId.of("t"),
            runId = RunId.of("r")
        )

        val json = AgUiJson.asFormatString(input)

        assertThat(json, containsSubstring("\"threadId\""))
        assertThat(json, containsSubstring("\"runId\""))
    }

    @Test
    fun `AgUiMessage roundtrips with toolCalls`() {
        val message = AgUiMessage(
            id = MessageId.of("m-1"),
            role = Role.Assistant,
            toolCalls = listOf(
                ToolCall(
                    id = ToolCallId.of("call-1"),
                    function = ToolCallFunction(
                        name = ToolName.of("lookup"),
                        arguments = """{"q":"http4k"}"""
                    )
                )
            )
        )

        val json = AgUiJson.asFormatString(message)
        assertThat(AgUiJson.asA<AgUiMessage>(json), equalTo(message))
        assertThat(json, containsSubstring("\"toolCalls\""))
    }

    @Test
    fun `parsed JSON object has the expected discriminator and field names`() {
        val input = RunAgentInput(threadId = ThreadId.of("t"), runId = RunId.of("r"))
        val node = AgUiJson.parse(AgUiJson.asFormatString(input)) as MoshiObject
        val keys = node.attributes.keys
        assertThat(keys.contains("threadId"), equalTo(true))
        assertThat(keys.contains("runId"), equalTo(true))
    }
}
