package org.http4k.wiretap.chaos

import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.html
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel
import org.http4k.wiretap.util.Json

data class ChaosSignals(
    val inBehaviour: String = "ReturnStatus",
    val inLatencyMin: Int = 100,
    val inLatencyMax: Int = 500,
    val inStatusCode: Int = 500,
    val inTrigger: String = "Always",
    val inPercentage: Int = 50,
    val inCountdown: Int = 5,
    val inMethod: String? = null,
    val inPath: String? = null,
    val inHost: String? = null,
    val outBehaviour: String = "ReturnStatus",
    val outLatencyMin: Int = 100,
    val outLatencyMax: Int = 500,
    val outStatusCode: Int = 500,
    val outTrigger: String = "Always",
    val outPercentage: Int = 50,
    val outCountdown: Int = 5,
    val outMethod: String? = null,
    val outPath: String? = null,
    val outHost: String? = null
)

fun Index(templates: TemplateRenderer): RoutingHttpHandler =
    "/" bind GET to { Response(OK).html(templates(Index())) }

data class Index(
    val initialSignals: String = Json.asDatastarSignals(ChaosSignals())
) : ViewModel
