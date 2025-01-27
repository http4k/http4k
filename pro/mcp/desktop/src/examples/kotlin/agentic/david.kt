package agentic

import agentic.tools.person.PersonToolPack
import org.http4k.core.PolyHandler
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.routing.mcpHttp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

fun david(): PolyHandler =
    mcpHttp(
        ServerMetaData(McpEntity.of("David"), Version.of("0.0.1")),
        PersonToolPack("David", ::david)
    )

private fun david(yearMonth: YearMonth) =
    yearMonth.atDay(1).datesUntil(yearMonth.plusMonths(1).atDay(1)).toList()
        .associateWith { date ->
            when (date) {
                LocalDate.of(2025, 3, 21) -> listOf() // Free Friday
                LocalDate.of(2025, 3, 22) -> listOf() // Free Saturday
                else -> when (date.dayOfWeek) {
                    DayOfWeek.FRIDAY, DayOfWeek.SATURDAY -> listOf(
                        "19:00 - Dinner with friends",
                    )

                    else -> listOf(
                        "10:00 - Gym",
                        "14:00 - Coffee with Sarah",
                        "18:00 - Team call",
                    )
                }
            }
        }
