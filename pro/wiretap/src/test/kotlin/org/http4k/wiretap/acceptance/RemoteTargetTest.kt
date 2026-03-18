/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
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
import org.http4k.wiretap.RemoteTarget
import org.http4k.wiretap.WiretapTarget
import org.junit.jupiter.api.BeforeEach

class RemoteTargetTest : WiretapSmokeContract {

    override val testRequest = Request(GET, Uri.of("/foo"))

    private val server = routes("/foo" bind GET to { _: Request -> Response(OK) })
        .asServer(Helidon(0))

    override lateinit var target: WiretapTarget

    @BeforeEach
    fun start() {
        server.start()
        target = RemoteTarget(server.uri())
    }

    @BeforeEach
    fun stop() {
        server.stop()
    }
}
