package org.http4k.wiretap

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.ToolRequest
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.ai.mcp.testing.testMcpClient
import org.http4k.ai.model.ToolName
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.format.Jackson
import org.http4k.routing.mcp
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
interface McpWiretapFunctionContract {

    val toolName: String
    val function: WiretapFunction

    fun mcpClient() = mcp(
        ServerMetaData("entity", "version"),
        NoMcpSecurity,
        function.mcp()
    ).testMcpClient(Request.Companion(Method.GET, "/mcp"))

    fun callTool(args: Map<String, Any>) = mcpClient().tools().call(
        ToolName.of(toolName),
        ToolRequest(args)
    )

    fun printTool() = println(
        mcpClient().tools().list()
            .valueOrNull()!!.find { it.name.value == toolName }
    )

    fun Approver.assertToolResponse(args: Map<String, Any>) {
        when (val result = callTool(args)) {
            is Success<ToolResponse> -> assertApproved(
                Jackson.prettify(Jackson.asFormatString(result.value))
            )

            is Failure<*> -> {
                println(result)
                TODO()
            }
        }
    }

}
