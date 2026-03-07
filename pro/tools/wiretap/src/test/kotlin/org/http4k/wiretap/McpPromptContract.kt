package org.http4k.wiretap

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.ai.mcp.PromptRequest
import org.http4k.ai.mcp.PromptResponse
import org.http4k.ai.mcp.model.PromptName
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.server.capability.PromptCapability
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.ai.mcp.testing.testMcpClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.format.Jackson
import org.http4k.routing.mcpHttpStreaming
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
interface McpPromptContract {

    val promptName: String
    val prompt: PromptCapability

    fun mcpClient() = mcpHttpStreaming(
        ServerMetaData("entity", "version"),
        NoMcpSecurity,
        prompt
    ).testMcpClient(Request(Method.GET, "/mcp"))

    fun Approver.assertPromptResponse(args: Map<String, String> = emptyMap()) {
        when (val result = mcpClient().prompts().get(PromptName.of(promptName), PromptRequest(args))) {
            is Success<PromptResponse> -> assertApproved(
                Jackson.prettify(Jackson.asFormatString(result.value))
            )

            is Failure<*> -> {
                println(result)
                TODO()
            }
        }
    }
}
