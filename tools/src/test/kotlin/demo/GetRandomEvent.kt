@file:Suppress("FunctionName")

package demo

import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.server.capability.ToolCapability
import org.http4k.ai.mcp.util.McpJson.auto
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.lens.with
import org.http4k.routing.bind
import java.time.LocalDate

private val eventOutput = Tool.Output.auto(Event("KotlinConf 2026", LocalDate.of(2026, 6, 3))).toLens()
private val eventLens = Body.auto<Event>().toLens()

fun GetRandomEvent(eventsClient: HttpHandler): ToolCapability =
    Tool("get_random_event", "Fetches a random upcoming event", output = eventOutput) bind {
        val event = eventLens(eventsClient(Request(GET, "/random-event")))
        ToolResponse.Ok().with(eventOutput of event)
    }
