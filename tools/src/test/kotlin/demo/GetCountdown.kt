@file:Suppress("FunctionName")

package demo

import org.http4k.ai.mcp.ToolRequest
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.localDate
import org.http4k.ai.mcp.model.string
import org.http4k.ai.mcp.server.capability.ToolCapability
import org.http4k.ai.mcp.util.McpJson.auto
import org.http4k.lens.with
import org.http4k.routing.bind
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS
import kotlin.time.Duration.Companion.days

val eventName = Tool.Arg.string().required("name", "the name of the event")
val date = Tool.Arg.localDate().required("date", "the date of the event")
val output = Tool.Output.auto(Countdown(1, "a structured message")).toLens()

data class Countdown(val days: Long, val message: String)

fun GetCountdown(clock: Clock): ToolCapability =
    Tool("countdown", "Builds excitement!", date, eventName, output = output) bind { req: ToolRequest ->
        val eventName: String = eventName(req)
        val targetDate: LocalDate = date(req)
        val datesUntilEvent: Long = DAYS.between(LocalDate.now(clock), targetDate).days.inWholeDays

        ToolResponse.Ok().with(output of Countdown(datesUntilEvent, "Only $datesUntilEvent days until $eventName!"))
    }
