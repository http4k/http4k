package agentic.tools.restaurant

import org.http4k.lens.int
import org.http4k.lens.localDate
import org.http4k.lens.localTime
import org.http4k.mcp.ToolResponse
import org.http4k.mcp.capability.ToolCapability
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.Tool
import org.http4k.routing.bind
import java.time.LocalDate

fun BookMeal(name: String): ToolCapability {
    val dateArg = Tool.Arg.localDate().required("date")
    val peopleArg = Tool.Arg.int().required("people")
    val timeArg = Tool.Arg.localTime().required("time")
    return Tool(
        "$name Book meal", "Books a slot for a given date, time, and number of people at $name",
        dateArg, peopleArg, timeArg,
    ) bind {
        val date = dateArg(it)
        val people = peopleArg(it)
        val time = timeArg(it)
        val confirmation =
            if (people == 3 && (date == LocalDate.of(2025, 3, 21) || date == LocalDate.of(2025, 3, 22))) {
                Content.Text("Booking confirmed for $people people on $date at $time")
            } else {
                Content.Text("Booking failed: No availability for $people people on $date at $time")
            }
        ToolResponse.Ok(listOf(confirmation))
    }
}
