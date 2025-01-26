package agentic

import agentic.tools.person.PersonToolPack
import org.http4k.core.PolyHandler
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.routing.mcpHttp
import java.time.DayOfWeek
import java.time.LocalDate

fun al(): PolyHandler =
    mcpHttp(
        ServerMetaData(McpEntity.of("al"), Version.of("0.0.1")),
        PersonToolPack("Al", ::al)
    )

private fun al(date: LocalDate): List<Content.Text> = when (date) {
    LocalDate.of(2025, 3, 21) -> listOf() // Free Friday
    LocalDate.of(2025, 3, 22) -> listOf() // Free Saturday
    else -> when (date.dayOfWeek) {
        DayOfWeek.FRIDAY, DayOfWeek.SATURDAY -> listOf(
            Content.Text("19:00 - Dinner with friends"),
        )

        else -> listOf(
            Content.Text("08:00 - Breakfast meeting"),
            Content.Text("11:00 - Dentist appointment"),
            Content.Text("16:00 - Project review"),
        )
    }
}
