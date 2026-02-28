package org.http4k.wiretap.chaos

import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.enum
import org.http4k.ai.mcp.model.int
import org.http4k.ai.mcp.model.string
import org.http4k.ai.mcp.server.capability.ToolCapability
import org.http4k.chaos.ChaosEngine
import org.http4k.core.Method
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.Selector
import org.http4k.lens.Path
import org.http4k.lens.datastarElements
import org.http4k.lens.enum
import org.http4k.routing.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.WiretapFunction
import org.http4k.wiretap.domain.ChaosConfig
import org.http4k.wiretap.domain.Direction
import org.http4k.wiretap.domain.Direction.Inbound
import org.http4k.wiretap.domain.Direction.Outbound
import org.http4k.wiretap.util.Json.datastarModel

fun ChaosActivate(inboundChaos: ChaosEngine, outboundChaos: ChaosEngine) = object : WiretapFunction {
    private fun activate(direction: Direction, config: ChaosConfig) {
        when (direction) {
            Inbound -> inboundChaos
            Outbound -> outboundChaos
        }.enable(config.toStage())
    }

    override fun http(elements: DatastarElementRenderer, html: TemplateRenderer) =
        "/{direction}/activate" bind POST to { req ->
            val direction = Path.enum<Direction>().of("direction")(req)
            val config = req.datastarModel<ChaosConfigSignals>().toChaosConfig()
            activate(direction, config)

            val view = ChaosStatusView(chaosStatus(inboundChaos, outboundChaos))
            Response(OK).datastarElements(
                elements(view),
                selector = Selector.of("#chaos-status")
            )
        }

    override fun mcp(): ToolCapability {
        val direction =
            Tool.Arg.enum<Direction>().required("direction", "Direction to apply chaos: Inbound or Outbound")
        val behaviour = Tool.Arg.string()
            .optional("behaviour", "Chaos behaviour: ReturnStatus, Latency, or NoBody (default: ReturnStatus)")
        val statusCode = Tool.Arg.int().map({ Status(it, null) }, Status::code)
            .defaulted("status_code", INTERNAL_SERVER_ERROR, "HTTP status code to return (default: 500)")
        val trigger = Tool.Arg.string()
            .optional(
                "trigger",
                "Trigger type: Always, MatchRequest, PercentageBased, Once, Countdown, or Delay (default: Always)"
            )
        val percentage = Tool.Arg.int().optional("percentage", "Percentage for PercentageBased trigger (default: 50)")
        val delaySeconds = Tool.Arg.int()
            .optional("delay_seconds", "Seconds to delay before activating chaos for Delay trigger (default: 10)")
        val method =
            Tool.Arg.enum<Method>().optional("method", "Only apply chaos to requests matching this HTTP method")
        val path = Tool.Arg.string().optional("path", "Only apply chaos to requests matching this path substring")
        val host = Tool.Arg.string().optional("host", "Only apply chaos to requests matching this host substring")

        return Tool(
            "chaos_activate",
            "Enable chaos injection on inbound or outbound traffic",
            direction, behaviour, statusCode, trigger, percentage, delaySeconds, method, path, host
        ) bind { req ->
            val dir = direction(req)
            val config = ChaosConfig(
                behaviour = behaviour(req) ?: "ReturnStatus",
                statusCode = statusCode(req),
                trigger = trigger(req) ?: "Always",
                percentage = percentage(req) ?: 50,
                delaySeconds = delaySeconds(req) ?: 10,
                method = method(req),
                path = path(req),
                host = host(req)
            )

            activate(dir, config)
            ToolResponse.Ok(listOf(Content.Text("Chaos activated on ${dir.name}: ${config.behaviour} with ${config.trigger} trigger")))

        }
    }
}
