package org.http4k.ai.mcp.conformance.server.tools

import dev.forkhandles.result4k.get
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import org.http4k.ai.mcp.ElicitationResponse
import org.http4k.ai.mcp.ElicitationResponse.Task
import org.http4k.ai.mcp.SamplingRequest
import org.http4k.ai.mcp.SamplingResponse
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.ToolResponse.Error
import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.model.Content.Text
import org.http4k.ai.mcp.model.Message
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.ToolChoice
import org.http4k.ai.mcp.model.ToolChoiceMode
import org.http4k.ai.mcp.model.string
import org.http4k.ai.model.MaxTokens
import org.http4k.ai.model.Role.Companion.User
import org.http4k.routing.bind

val prompt = Tool.Arg.string().required("prompt")

fun samplingTool() = Tool("test_sampling", "test_sampling", prompt) bind {
    it.client.sample(
        SamplingRequest(
            messages = listOf(Message(User, Text(prompt(it)))),
            maxTokens = MaxTokens.of(10000),
            tools = emptyList(),
            toolChoice = ToolChoice(ToolChoiceMode.auto)
        )
    ).toList()
        .first()
        .map {
            when (it) {
                is SamplingResponse.Ok -> Ok(it.content.toString())
                is SamplingResponse.Task -> error("Unexpected task response")
            }
        }
        .mapFailure { Error("Problem with response") }
        .get()
}

