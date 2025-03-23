@file:Suppress("unused")

package server.serverless

import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.routing.mcpHttpNonStreaming
import org.http4k.serverless.ApiGatewayV2LambdaFunction
import org.http4k.serverless.AppLoader
import server.completions
import server.prompts
import server.resources
import server.tools

/**
 * This example demonstrates how to create an MCP Lambda function using the non-streaming HTTP protocol.
 */
class McpLambdaFunction : ApiGatewayV2LambdaFunction(AppLoader {
    mcpHttpNonStreaming(
        ServerMetaData(McpEntity.of("http4k mcp over serverless"), Version.of("0.1.0")),
        prompts(),
        resources(),
        tools(),
        completions()
    )
})
