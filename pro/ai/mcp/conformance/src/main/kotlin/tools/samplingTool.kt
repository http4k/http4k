package tools

import dev.forkhandles.result4k.get
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import org.http4k.ai.mcp.SamplingRequest
import org.http4k.ai.mcp.ToolResponse.Error
import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.model.Content.Text
import org.http4k.ai.mcp.model.Message
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.string
import org.http4k.ai.model.MaxTokens
import org.http4k.ai.model.Role.Companion.User
import org.http4k.routing.bind

val prompt = Tool.Arg.string().required("prompt")

fun samplingTool() = Tool("test_sampling", "test_sampling", prompt) bind {
    it.client.sample(SamplingRequest(listOf(Message(User, Text(prompt(it)))), MaxTokens.of(10000)))
        .toList()
        .first()
        .map { Ok(it.content.toString()) }
        .mapFailure { Error(1, "Problem with response") }
        .get()
}

