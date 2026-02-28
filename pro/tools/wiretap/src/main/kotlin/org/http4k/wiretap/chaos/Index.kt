package org.http4k.wiretap.chaos

import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.html
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel
import org.http4k.core.Method
import org.http4k.core.Status
import org.http4k.wiretap.domain.ChaosConfig
import org.http4k.wiretap.util.Json

data class ChaosConfigSignals(
    val behaviour: String = "ReturnStatus",
    val latencyMin: Int = 100,
    val latencyMax: Int = 500,
    val statusCode: Int = 500,
    val trigger: String = "Always",
    val percentage: Int = 50,
    val countdown: Int = 5,
    val delaySeconds: Int = 10,
    val method: String? = null,
    val path: String? = null,
    val host: String? = null
) {
    fun toChaosConfig() = ChaosConfig(
        behaviour = behaviour,
        latencyMin = latencyMin,
        latencyMax = latencyMax,
        statusCode = Status(statusCode, null),
        trigger = trigger,
        percentage = percentage,
        countdown = countdown,
        delaySeconds = delaySeconds,
        method = method?.takeIf { it.isNotEmpty() }?.let { Method.valueOf(it) },
        path = path,
        host = host
    )
}

fun Index(templates: TemplateRenderer): RoutingHttpHandler =
    "/" bind GET to { Response(OK).html(templates(Index())) }

data class Index(
    val initialSignals: String = Json.asDatastarSignals(ChaosConfigSignals())
) : ViewModel
