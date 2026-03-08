package org.http4k.wiretap.acceptance

import org.http4k.core.ContentType
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.lens.accept
import org.http4k.routing.poly
import org.http4k.routing.sse
import org.http4k.routing.sse.bind
import org.http4k.server.Helidon
import org.http4k.server.asServer
import org.http4k.server.uri
import org.http4k.sse.SseMessage
import org.http4k.sse.SseResponse
import org.http4k.wiretap.WiretappedUriProvider
import org.junit.jupiter.api.BeforeEach

class PolyServerTest : WiretapSmokeContract {

    override val testRequest = Request(GET, Uri.of("/foo"))
        .accept(ContentType.TEXT_EVENT_STREAM)

    private val server = sse("/foo" bind {
        SseResponse { it.send(SseMessage.Event("")).close() }
    })
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
