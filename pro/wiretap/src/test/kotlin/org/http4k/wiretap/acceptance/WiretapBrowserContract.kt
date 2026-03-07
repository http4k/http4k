package org.http4k.wiretap.acceptance

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.playwright.LaunchPlaywrightBrowser
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.http4k.server.uri
import org.http4k.wiretap.Wiretap
import org.junit.jupiter.api.extension.RegisterExtension

abstract class WiretapBrowserContract {
    val app = { req: Request -> Response(OK) }

    @RegisterExtension
    val playwright = LaunchPlaywrightBrowser(Wiretap { _, _ ->
        app.asServer(Jetty(0)).start().uri()
    }, ::Jetty)
}

