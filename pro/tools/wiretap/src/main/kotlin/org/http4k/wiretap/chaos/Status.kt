package org.http4k.wiretap.chaos

import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.server.capability.ToolCapability
import org.http4k.chaos.ChaosEngine
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.Selector
import org.http4k.lens.datastarElements
import org.http4k.routing.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel
import org.http4k.wiretap.WiretapFunction
import org.http4k.wiretap.domain.ChaosStatusData
import org.http4k.wiretap.util.Json

fun ChaosStatus(inboundChaos: ChaosEngine, outboundChaos: ChaosEngine) = object : WiretapFunction {
    override fun http(elements: DatastarElementRenderer, html: TemplateRenderer) = "/status" bind GET to {
        val view = ChaosStatusView(chaosStatus(inboundChaos, outboundChaos))
        Response(OK).datastarElements(
            elements(view),
            selector = Selector.of("#chaos-status")
        )
    }

    override fun mcp(): ToolCapability = Tool(
        "chaos_status",
        "Get the current status of inbound and outbound chaos engines"
    ) bind {
        Json.asToolResponse(chaosStatus(inboundChaos, outboundChaos))
    }
}

data class ChaosStatusView(val data: ChaosStatusData) : ViewModel {
    val inboundBadgeClass = if (data.inboundActive) "badge-chaos-active" else "badge-chaos-inactive"
    val inboundBadgeText = if (data.inboundActive) "ACTIVE" else "INACTIVE"
    val outboundBadgeClass = if (data.outboundActive) "badge-chaos-active" else "badge-chaos-inactive"
    val outboundBadgeText = if (data.outboundActive) "ACTIVE" else "INACTIVE"
}

internal fun chaosStatus(inboundChaos: ChaosEngine, outboundChaos: ChaosEngine) = ChaosStatusData(
    inboundActive = inboundChaos.isEnabled(),
    inboundDescription = inboundChaos.toString(),
    outboundActive = outboundChaos.isEnabled(),
    outboundDescription = outboundChaos.toString()
)
