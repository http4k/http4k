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

enum class WiretapExamples(val fn: () -> PolyHandler) : () -> PolyHandler by fn {

    httpApp({ Wiretap(HttpApp().asServer(Jetty(0)).start().uri()) }),

    httpAppWithOtelTracing({
        val clientApp = HttpApp().asServer(Jetty(0)).start()

        Wiretap { http, oTel ->
            HttpAppWithOtelTracing(clientApp.uri(), http, oTel).asServer(Jetty(0)).start().uri()
        }
    }),

    mcpApp({ Wiretap(McpApp().asServer(Jetty(0)).start().uri()) }),

    mcpServer({ Wiretap(McpServer().asServer(Jetty(0)).start().uri()) }),

    mcpServerWithOtel({
        Wiretap { http, otel ->
            McpServerWithOtelTracing(http, otel).asServer(Jetty(0)).start().uri()
        }
    }),

    openApiApp({ Wiretap(OpenApiApp().asServer(Jetty(0)).start().uri()) }),

    externalMcpAppUrl({ Wiretap(Uri.Companion.of("https://demo.http4k.org/mcp-app")) }),

    website({ Wiretap(Uri.Companion.of("https://http4k.org")) })
}
