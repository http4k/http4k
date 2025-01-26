package agentic

import agentic.tools.person.PersonToolPack
import org.http4k.core.PolyHandler
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.routing.mcpHttp
import java.time.LocalDate

fun david(): PolyHandler =
    mcpHttp(
        ServerMetaData(McpEntity.of("David"), Version.of("0.0.1")),
        PersonToolPack("David", ::david)
    )

private fun david(date: LocalDate): List<Content.Text> = when (date) {
    LocalDate.of(2025, 3, 21) -> listOf() // Free Friday
    LocalDate.of(2025, 3, 22) -> listOf() // Free Saturday
    else -> when (date.dayOfWeek) {
        java.time.DayOfWeek.FRIDAY, java.time.DayOfWeek.SATURDAY -> listOf(
            Content.Text("19:00 - Dinner with friends"),
        )

        else -> listOf(
            Content.Text("10:00 - Gym"),
            Content.Text("14:00 - Coffee with Sarah"),
            Content.Text("18:00 - Team call"),
        )
    }
}
