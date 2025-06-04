package chatzilla

import org.http4k.mcp.ToolResponse.Ok
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.model.Tool
import org.http4k.mcp.model.string
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.mcp.server.security.NoMcpSecurity
import org.http4k.routing.bind
import org.http4k.routing.mcpHttpStreaming
import org.http4k.server.Http4kServer
import org.http4k.server.JettyLoom
import org.http4k.server.asServer

val name = Tool.Arg.string().required("name")

val getFullNameTool = Tool("getFullName", "get the full name", name)
val greetingTool = Tool("greeting", "greet a person by name", name)

fun mcpServer(): Http4kServer {

    return mcpHttpStreaming(
        ServerMetaData(McpEntity.of("123"), Version.of("123")),
        NoMcpSecurity,
        getFullNameTool bind { Ok("${name(it)} Smith") },
        greetingTool bind { Ok("hello ${name(it)}") }
    ).asServer(JettyLoom(0))
}
