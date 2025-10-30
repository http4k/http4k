package chatzilla

import chatzilla.Settings.MCP_URL
import chatzilla.Settings.MODEL
import org.http4k.config.Environment.Companion.ENV
import org.http4k.connect.anthropic.AnthropicModels.Claude_Haiku_4_5
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.server.JettyLoom
import org.http4k.server.asServer


fun main() {
    val mcp = mcpServer(8000).start()

    val env = ENV
        .with(MCP_URL of Uri.of("http://localhost:${mcp.port()}/mcp"))
        .with(MODEL of Claude_Haiku_4_5)
    Chatzilla(env).asServer(JettyLoom(9000)).start()

    println("Open http://localhost:9000/ in your browser to use the chat interface")
}
