package chatzilla

import org.http4k.core.Uri
import org.http4k.server.JettyLoom
import org.http4k.server.asServer


fun main() {
    val mcp = mcpServer(8000).start()

    Chatzilla(Uri.of("http://localhost:${mcp.port()}/mcp")).asServer(JettyLoom(9000)).start()

    println("Open http://localhost:9000/ in your browser to use the chat interface")
}
