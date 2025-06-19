package agentic.tools.restaurant

import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.int
import org.http4k.ai.mcp.model.localDate
import org.http4k.ai.mcp.model.localTime
import org.http4k.ai.mcp.server.capability.ToolCapability
import org.http4k.routing.bind
import java.time.LocalDate

fun BookMeal(name: String): ToolCapability {
    val dateArg = Tool.Arg.localDate().required("date", "date for the booking. in format yyyy-mm-dd")
    val peopleArg = Tool.Arg.int().required("people", "how many people are in the party")
    val timeArg = Tool.Arg.localTime().required("time", "time for the booking. in the format 10:15")
    return Tool(
        "${name.filter { it.isLetterOrDigit() }}_Book_meal",
        "Books a slot for a given date, time, and number of people at $name",
        dateArg,
        peopleArg,
        timeArg,
    ) bind { req ->
        val date = dateArg(req)
        val people = peopleArg(req)
        val time = timeArg(req)
        val confirmation =
            if (people == 3 && (date == LocalDate.of(2025, 5, 10))) {
                Content.Text("Booking confirmed for $people people on $date at $time")
            } else {
                Content.Text("Booking failed: No availability for $people people on $date at $time")
            }
        ToolResponse.Ok(listOf(confirmation))
    }
}
