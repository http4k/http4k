package org.http4k.k8s

import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE

typealias ReadinessCheckResultRenderer = (result: ReadinessCheckResult) -> Response

object DefaultReadinessCheckResultRenderer : ReadinessCheckResultRenderer {
    override fun invoke(result: ReadinessCheckResult): Response =
            Response(if (result.pass) OK else SERVICE_UNAVAILABLE).body(result.toString())
}
