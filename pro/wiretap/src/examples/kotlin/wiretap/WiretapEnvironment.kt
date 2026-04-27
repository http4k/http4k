/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package wiretap

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
    override fun invoke() = Wiretap(RemoteTarget {
        HttpApp().asServer(Jetty(0)).start().uri()
    })
}

object HttpAppWithOtelTracing : WiretapEnvironment {
    override fun invoke(): PolyHandler {
        val clientApp = HttpApp().asServer(Jetty(0)).start()
        return Wiretap(RemoteTarget {
            HttpAppWithOtelTracing(clientApp.uri(), http()).asServer(Jetty(0)).start().uri()
        })
    }
}

object LocalHttpAppWithOtelTracing : WiretapEnvironment {
    override fun invoke(): PolyHandler {
        val clientApp = HttpApp().asServer(Jetty(0)).start()
        return Wiretap(LocalTarget {
            HttpAppWithOtelTracing(clientApp.uri(), http())
        })
    }
}

object McpApp : WiretapEnvironment {
    override fun invoke() = Wiretap(RemoteTarget {
        McpApp().asServer(Jetty(0)).start().uri()
    })
}

object McpServer : WiretapEnvironment {
    override fun invoke() = Wiretap(RemoteTarget {
        McpServer().asServer(Jetty(0)).start().uri()
    })
}

object McpServerWithOtel : WiretapEnvironment {
    override fun invoke() = Wiretap(RemoteTarget {
        McpServerWithOtelTracing(http()).asServer(Jetty(0)).start().uri()
    })
}

object LocalMcpServerWithOtel : WiretapEnvironment {
    override fun invoke() = Wiretap(LocalTarget {
        McpServerWithOtelTracing(http()).http!!
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
