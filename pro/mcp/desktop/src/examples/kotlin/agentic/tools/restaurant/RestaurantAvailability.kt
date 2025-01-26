package agentic.tools.restaurant

import org.http4k.lens.int
import org.http4k.lens.localDate
import org.http4k.mcp.ToolResponse
import org.http4k.mcp.capability.ToolCapability
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.Tool
import org.http4k.routing.bind
import java.time.LocalDate

fun RestaurantAvailability(name: String): ToolCapability {
    val dateArg = Tool.Arg.localDate().required("date", "date in format yyyy-mm-dd")
    val peopleArg = Tool.Arg.int().required("people", "how many people are in the party")
    return Tool(
        "$name Resraurant availability", "for restaurant $name, provides a list of available slots for a given number of people on a specified date",
        dateArg,
        peopleArg
    ) bind {
        val date: LocalDate = dateArg(it)
        val people: Int = peopleArg(it)

        val slots = when {
            date == LocalDate.of(2025, 3, 22) -> listOf( // Free slots on specific dates
                Content.Text("18:00 - Available for $people people"),
                Content.Text("19:00 - Available for $people people"),
                Content.Text("20:00 - Available for $people people")
            )

            date.dayOfWeek == java.time.DayOfWeek.FRIDAY || date.dayOfWeek == java.time.DayOfWeek.SATURDAY -> listOf( // Limited availability
                Content.Text("18:00 - Fully booked"),
                Content.Text("19:00 - Fully booked"),
                Content.Text("20:00 - Fully booked")
            )

            else -> listOf( // Regular availability
                Content.Text("12:00 - Available for $people people"),
                Content.Text("13:00 - Available for $people people"),
                Content.Text("19:00 - Available for $people people")
            )
        }

        ToolResponse.Ok(slots)
    }
}
