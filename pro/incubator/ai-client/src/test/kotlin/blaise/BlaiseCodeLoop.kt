package blaise

import mcp.mcpServer
import org.http4k.core.Uri

fun main() {
    val mcp = mcpServer(8000).start()
    BlaiseCode(Uri.of("http://localhost:${mcp.port()}/mcp"), object : IO {
        override fun read() = readln()
        override fun write(s: String) = println(s)
    })
}
