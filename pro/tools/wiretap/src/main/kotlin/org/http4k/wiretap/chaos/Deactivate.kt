package org.http4k.wiretap.chaos

import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.enum
import org.http4k.ai.mcp.server.capability.ToolCapability
import org.http4k.chaos.ChaosEngine
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.Selector
import org.http4k.lens.Path
import org.http4k.lens.enum
import org.http4k.lens.datastarElements
import org.http4k.routing.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.WiretapFunction
import org.http4k.wiretap.domain.Direction
import org.http4k.wiretap.domain.Direction.Inbound
import org.http4k.wiretap.domain.Direction.Outbound

fun ChaosDeactivate(inboundChaos: ChaosEngine, outboundChaos: ChaosEngine) = object : WiretapFunction {
    private fun deactivate(direction: Direction) {
        when (direction) {
            Inbound -> inboundChaos
            Outbound -> outboundChaos
        }.disable()
    }

    override fun http(elements: DatastarElementRenderer, html: TemplateRenderer) =
        "/{direction}/deactivate" bind POST to { req ->
            val direction = Path.enum<Direction>().of("direction")(req)
            deactivate(direction)

            val view = ChaosStatusView(chaosStatus(inboundChaos, outboundChaos))
            Response(OK).datastarElements(
                elements(view),
                selector = Selector.of("#chaos-status")
            )
        }

    override fun mcp(): ToolCapability {
        val direction =
            Tool.Arg.enum<Direction>().required("direction", "Direction to deactivate chaos: Inbound or Outbound")

        return Tool(
            "chaos_deactivate",
            "Disable chaos injection on inbound or outbound traffic",
            direction
        ) bind { req ->
            val dir = direction(req)
            deactivate(dir)
            ToolResponse.Ok(listOf(Content.Text("Chaos deactivated on ${dir.name}")))
        }
    }
}
