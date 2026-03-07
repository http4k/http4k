package wiretap

import org.http4k.core.PolyHandler
import org.http4k.core.Uri
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.http4k.server.uri
import org.http4k.wiretap.Wiretap
import wiretap.examples.HttpApp
import wiretap.examples.HttpAppWithOtelTracing
import wiretap.examples.McpApp
import wiretap.examples.McpServer
import wiretap.examples.McpServerWithOtelTracing
import wiretap.examples.OpenApiApp

sealed class WiretapEnvironment : () -> PolyHandler

object HttpApp : WiretapEnvironment() {
    override fun invoke() = Wiretap(HttpApp().asServer(Jetty(0)).start().uri())
}

object HttpAppWithOtelTracing : WiretapEnvironment() {
    override fun invoke(): PolyHandler {
        val clientApp = HttpApp().asServer(Jetty(0)).start()
        return Wiretap { http, oTel ->
            HttpAppWithOtelTracing(clientApp.uri(), http, oTel).asServer(Jetty(0)).start().uri()
        }
    }
}

object McpApp : WiretapEnvironment() {
    override fun invoke() = Wiretap(McpApp().asServer(Jetty(0)).start().uri())
}

object McpServer : WiretapEnvironment() {
    override fun invoke() = Wiretap(McpServer().asServer(Jetty(0)).start().uri())
}

object McpServerWithOtel : WiretapEnvironment() {
    override fun invoke() = Wiretap { http, otel ->
        McpServerWithOtelTracing(http, otel).asServer(Jetty(0)).start().uri()
    }
}

object OpenApiApp : WiretapEnvironment() {
    override fun invoke() = Wiretap(OpenApiApp().asServer(Jetty(0)).start().uri())
}

object ExternalMcpAppUrl : WiretapEnvironment() {
    override fun invoke() = Wiretap(Uri.of("https://demo.http4k.org/mcp-app"))
}

object ExternalWebsite : WiretapEnvironment() {
    override fun invoke() = Wiretap(Uri.of("https://http4k.org"))
}
