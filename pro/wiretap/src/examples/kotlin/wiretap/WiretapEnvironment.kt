/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package wiretap

import io.opentelemetry.api.OpenTelemetry
import org.http4k.core.HttpHandler
import org.http4k.core.PolyHandler
import org.http4k.core.Uri
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.http4k.server.uri
import org.http4k.wiretap.LocalTarget
import org.http4k.wiretap.RemoteTarget
import org.http4k.wiretap.Wiretap
import wiretap.examples.HttpApp
import wiretap.examples.HttpAppWithOtelTracing
import wiretap.examples.McpApp
import wiretap.examples.McpServer
import wiretap.examples.McpServerWithOtelTracing
import wiretap.examples.OpenApiApp

sealed interface WiretapEnvironment : () -> PolyHandler

object HttpApp : WiretapEnvironment {
    override fun invoke() = Wiretap(RemoteTarget { _, _ ->
            HttpApp().asServer(Jetty(0)).start().uri()
    })
}

object HttpAppWithOtelTracing : WiretapEnvironment {
    override fun invoke(): PolyHandler {
        val clientApp = HttpApp().asServer(Jetty(0)).start()
        return Wiretap(RemoteTarget { http, oTel ->
            HttpAppWithOtelTracing(clientApp.uri(), http, oTel).asServer(Jetty(0)).start().uri()
        })
    }
}

object LocalHttpAppWithOtelTracing : WiretapEnvironment {
    override fun invoke(): PolyHandler {
        val clientApp = HttpApp().asServer(Jetty(0)).start()
        return Wiretap(LocalTarget { http, oTel ->
            HttpAppWithOtelTracing(clientApp.uri(), http, oTel)
        })
    }
}

object McpApp : WiretapEnvironment {
    override fun invoke() = Wiretap(RemoteTarget { _: HttpHandler, _: OpenTelemetry ->
        McpApp().asServer(Jetty(0)).start().uri()
    })
}

object McpServer : WiretapEnvironment {
    override fun invoke() = Wiretap(RemoteTarget { _: HttpHandler, _: OpenTelemetry ->
        McpServer().asServer(Jetty(0)).start().uri()
    })
}

object McpServerWithOtel : WiretapEnvironment {
    override fun invoke() = Wiretap(RemoteTarget { http: HttpHandler, otel: OpenTelemetry ->
        McpServerWithOtelTracing(http, otel).asServer(Jetty(0)).start().uri()
    })
}

object LocalMcpServerWithOtel : WiretapEnvironment {
    override fun invoke() = Wiretap(LocalTarget { http: HttpHandler, otel: OpenTelemetry ->
        McpServerWithOtelTracing(http, otel).http!!
    })
}

object OpenApiApp : WiretapEnvironment {
    override fun invoke() = Wiretap(
        RemoteTarget(OpenApiApp().asServer(Jetty(0)).start().uri())
    )
}

object ExternalMcpServer : WiretapEnvironment {
    override fun invoke() = Wiretap(
        RemoteTarget(Uri.of("https://demo.http4k.org/mcp-sdk"))
    )
}

object ExternalMcpApp : WiretapEnvironment {
    override fun invoke() = Wiretap(
        RemoteTarget(Uri.of("https://demo.http4k.org/mcp-app"))
    )
}

object ExternalWebsite : WiretapEnvironment {
    override fun invoke() = Wiretap(
        RemoteTarget(Uri.of("https://demo.http4k.org"))
    )
}
