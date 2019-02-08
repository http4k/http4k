package org.http4k.cloudnative.health

import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes

/**
 * Represents the set of operational endpoints to ensure that a particular app is working ok.
 * By default provides Readiness and Liveness endpoints, but extra routes can be passed, as
 * can a different renderer implementation for the ReadinessCheck results.
 */
object Health {
    operator fun invoke(
        vararg extraRoutes: RoutingHttpHandler,
        checks: List<ReadinessCheck> = emptyList(),
        renderer: ReadinessCheckResultRenderer = DefaultReadinessCheckResultRenderer
    ) = routes(
        "/liveness" bind GET to Liveness,
        "/readiness" bind GET to Readiness(checks, renderer),
        *extraRoutes
    )
}

/**
 * The Liveness check is used to determine if an app is alive.
 */
object Liveness : HttpHandler {
    override fun invoke(request: Request) = Response(OK)
}

/**
 * The Readiness check is used to determine if an app is prepared to receive live traffic.
 */
object Readiness {
    operator fun invoke(
        checks: List<ReadinessCheck> = emptyList(),
        renderer: ReadinessCheckResultRenderer = DefaultReadinessCheckResultRenderer
    ) = HttpHandler {
        val overall: ReadinessCheckResult = when {
            checks.isNotEmpty() -> checks.map { check ->
                try {
                    check()
                } catch (e: Exception) {
                    Failed(check.name, e)
                }
            }.reduce { acc, result -> acc + result }
            else -> Completed("success")
        }
        Response(if (overall.pass) OK else SERVICE_UNAVAILABLE)
            .with(CONTENT_TYPE of renderer.contentType)
            .body(renderer(overall))
    }
}