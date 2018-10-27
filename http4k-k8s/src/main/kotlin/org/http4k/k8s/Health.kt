package org.http4k.k8s

import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.routes

object Health {
    operator fun invoke(
            renderer: ReadinessCheckResultRenderer = DefaultReadinessCheckResultRenderer,
            vararg checks: ReadinessCheck) = routes(
            "/liveness" bind GET to { Response(OK) },
            "/readiness" bind GET to {
                renderer(checks.fold(ReadinessCheckResult()) { acc, next -> acc + next() })
            }
    )
}