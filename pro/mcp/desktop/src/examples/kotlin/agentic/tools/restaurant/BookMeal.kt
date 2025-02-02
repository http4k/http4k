package agentic.tools.restaurant

import org.http4k.lens.int
import org.http4k.lens.localDate
import org.http4k.lens.localTime
import org.http4k.mcp.ToolResponse
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.Tool
import org.http4k.mcp.server.capability.ToolCapability
import org.http4k.routing.bind
import java.time.LocalDate

fun BookMeal(name: String): ToolCapability {
    val dateArg = Tool.Arg.localDate().required("date", "date for the booking. in format yyyy-mm-dd")
    val peopleArg = Tool.Arg.int().required("people", "how many people are in the party")
    val timeArg = Tool.Arg.localTime().required("time", "time for the booking. in the format 10:15")
    return Tool(
        "${name.filter { it.isLetterOrDigit() }}_Book_meal", "Books a slot for a given date, time, and number of people at $name",
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
