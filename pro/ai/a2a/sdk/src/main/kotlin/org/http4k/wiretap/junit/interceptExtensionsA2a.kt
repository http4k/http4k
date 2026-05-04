/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.junit

import org.http4k.core.Filter
import org.http4k.core.NoOp
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.OpenTelemetryTracing
import org.http4k.filter.PolyFilters
import org.http4k.protocol.A2A
import org.http4k.routing.a2aJsonRpc
import org.http4k.wiretap.Context
import java.security.SecureRandom
import java.time.Clock
import java.util.Random

/**
 * Intercept an A2A JSON RPC Server.
 */
fun Intercept.Companion.a2a(
    renderMode: RenderMode = RenderMode.OnFailure,
    redirectFilter: Filter = Filter.NoOp,
    clock: Clock = Clock.systemUTC(),
    random: Random = SecureRandom(byteArrayOf()),
    serverName: String = "http4k-server",
    baseUrl: Uri = Uri.of(""),
    appFn: Context.() -> A2A
) = poly(renderMode, redirectFilter, clock, random, serverName, baseUrl) {
    PolyFilters.OpenTelemetryTracing().then(a2aJsonRpc(appFn(), baseUrl.path))
}
