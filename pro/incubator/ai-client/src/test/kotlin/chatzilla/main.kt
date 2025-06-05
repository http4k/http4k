package chatzilla

import org.http4k.core.Uri
import org.http4k.filter.debug
import org.http4k.server.SunHttp
import org.http4k.server.asServer

fun main() {
    val server = mcpServer().start()

    val webServer = Chatzilla(Uri.of("http://localhost:${server.port()}/mcp"))
        .debug()
        .asServer(SunHttp(9000)).start()

    println("Chatzilla server started!")
    println("Open http://localhost:9000/ in your browser to use the chat interface")
    println("API endpoints are available at http://localhost:9000/api/*")

    // Keep server running until shutdown
    Runtime.getRuntime().addShutdownHook(Thread {
        webServer.stop()
        server.stop()
        println("Servers stopped")
    })
}
