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
import org.http4k.wiretap.LocalTarget

class LocalTargetTest : WiretapSmokeContract {

    override val testRequest = Request(GET, Uri.of("/foo"))

    override val target = LocalTarget(
        routes("/foo" bind GET to { _: Request -> Response(OK) })
    )
}
