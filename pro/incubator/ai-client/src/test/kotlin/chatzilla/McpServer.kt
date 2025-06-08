package chatzilla

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.onFailure
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.ElicitationRequest
import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.string
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.routing.bind
import org.http4k.routing.mcpHttpStreaming
import org.http4k.server.JettyLoom
import org.http4k.server.asServer

val name = Tool.Arg.string().required("name")

val getFullNameTool = Tool("getFullName", "get the full name", name)
val greetingTool = Tool("greeting", "greet a person by name", name)

fun mcpServer(port: Int = 0) = mcpHttpStreaming(
    ServerMetaData(McpEntity.of("123"), Version.of("123")),
    NoMcpSecurity,
    getFullNameTool bind {
        val firstName = name(it)
        System.err.println("SENDING ELICITATION FOR $it")
        it.client.elicit(ElicitationRequest(it.meta.progress!!))
            .map {
                System.err.println("RESPONSE ELICITATION WAS $it")
                Ok("$firstName '${it.action}' Smith")
            }.valueOrNull()!!
    },
    greetingTool bind { Ok("hello ${name(it)}") }
).asServer(JettyLoom(port))
