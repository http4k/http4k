package org.http4k.wiretap.junit

import org.http4k.core.Filter
import org.http4k.core.NoOp
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.OpenTelemetryTracing
import org.http4k.filter.ServerFilters
import org.http4k.protocol.A2A
import org.http4k.routing.a2aJsonRpc
import org.http4k.routing.a2aRest
import org.http4k.wiretap.Context
import java.security.SecureRandom
import java.time.Clock
import java.util.Random

/**
 * Intercept an A2A JSON RPC Server.
 */
fun Intercept.Companion.a2aJsonRpc(
    renderMode: RenderMode = RenderMode.OnFailure,
    redirectFilter: Filter = Filter.NoOp,
    clock: Clock = Clock.systemUTC(),
    random: Random = SecureRandom(byteArrayOf()),
    serverName: String = "http4k-server",
    baseUrl: Uri = Uri.of(""),
    appFn: Context.() -> A2A
) = http(renderMode, redirectFilter, clock, random, serverName, baseUrl) {
    ServerFilters.OpenTelemetryTracing().then(a2aJsonRpc(appFn(), ""))
}

/**
 * Intercept an A2A JSON RPC Server.
 */
fun Intercept.Companion.a2aRest(
    renderMode: RenderMode = RenderMode.OnFailure,
    redirectFilter: Filter = Filter.NoOp,
    clock: Clock = Clock.systemUTC(),
    random: Random = SecureRandom(byteArrayOf()),
    serverName: String = "http4k-server",
    baseUrl: Uri = Uri.of(""),
    appFn: Context.() -> A2A
) = http(renderMode, redirectFilter, clock, random, serverName, baseUrl) {
    ServerFilters.OpenTelemetryTracing().then(a2aRest(appFn(), ""))
}
