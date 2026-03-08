package org.http4k.wiretap.acceptance

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Helidon
import org.http4k.server.asServer
import org.http4k.server.uri
import org.http4k.wiretap.WiretappedUriProvider
import org.junit.jupiter.api.BeforeEach

class HttpServerTest : WiretapSmokeContract {

    override val testRequest = Request(GET, Uri.of("/foo"))

    private val server = routes("/foo" bind GET to { _: Request -> Response(OK) })
        .asServer(Helidon(0))

    override lateinit var uriProvider: WiretappedUriProvider

    @BeforeEach
    fun start() {
        server.start()
        uriProvider = WiretappedUriProvider { _, _ -> server.uri() }
    }

    @BeforeEach
    fun stop() {
        server.stop()
    }
}
