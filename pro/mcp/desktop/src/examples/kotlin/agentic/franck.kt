package agentic

import agentic.tools.person.PersonToolPack
import org.http4k.core.PolyHandler
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.routing.mcpHttp
import java.time.LocalDate

fun franck(): PolyHandler =
    mcpHttp(
        ServerMetaData(McpEntity.of("Franck"), Version.of("0.0.1")),
        PersonToolPack("Franck", ::franck)
    )

private fun franck(date: LocalDate): List<Content.Text> = when (date) {
    LocalDate.of(2025, 3, 21) -> listOf() // Free Friday
    LocalDate.of(2025, 3, 22) -> listOf() // Free Saturday
    else -> when (date.dayOfWeek) {
        java.time.DayOfWeek.FRIDAY, java.time.DayOfWeek.SATURDAY -> listOf(
            Content.Text("19:00 - Dinner with friends"),
        )

        else -> listOf(
            Content.Text("09:00 - Meeting"),
            Content.Text("12:00 - Lunch"),
            Content.Text("15:00 - Doctor's appointment"),
        )
    }
}
