package org.http4k.k8s

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.routing.bind
import org.http4k.routing.routes

/**
 * Represents the set of operational endpoints called by K8S to ensure that a particular pod is working ok.
 */
object Health {
    operator fun invoke(
        renderer: ReadinessCheckResultRenderer = DefaultReadinessCheckResultRenderer,
        checks: List<ReadinessCheck> = emptyList()) = routes(
        "/liveness" bind GET to liveness(),
        "/readiness" bind GET to readiness(checks, renderer)
    )

    /**
     * The Readiness check is used by K8S to determine if an app is prepared to receive live traffic.
     */
    private fun readiness(checks: List<ReadinessCheck>, renderer: ReadinessCheckResultRenderer): (Request) -> Response =
        {
            val overall = when {
                checks.isEmpty() -> ReadinessCheckResult(true)
                else -> checks.drop(1).fold(checks.first()()) { acc, function -> acc + function() }
            }
            Response(if (overall.pass) OK else SERVICE_UNAVAILABLE)
                .with(CONTENT_TYPE of renderer.contentType)
                .body(renderer(overall))

        }

    /**
     * The Liveness check is used by K8S to determine if an app is alive.
     */
    private fun liveness(): (Request) -> Response = { Response(OK) }
}