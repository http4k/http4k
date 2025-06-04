package agentic

import agentic.tools.person.PersonToolPack
import org.http4k.core.PolyHandler
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.routing.mcpHttpStreaming
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

fun al(): PolyHandler =
    mcpHttpStreaming(
        ServerMetaData(McpEntity.of("al"), Version.of("0.0.1")),
        NoMcpSecurity,
        PersonToolPack("Al", ::al)
    )

private fun al(yearMonth: YearMonth) =
    yearMonth.atDay(1).datesUntil(yearMonth.plusMonths(1).atDay(1)).toList()
        .associateWith { date ->
            when (date) {
                LocalDate.of(2025, 5, 9) -> listOf() // Free Friday
                LocalDate.of(2025, 5, 10) -> listOf() // Free Saturday
                else -> when (date.dayOfWeek) {
                    DayOfWeek.FRIDAY, DayOfWeek.SATURDAY -> listOf(
                        "19:00 - Dinner with friends",
                    )

                    else -> listOf(
                        "08:00 - Breakfast meeting",
                        "11:00 - Dentist appointment",
                        "16:00 - Project review",
                    )
                }
            }
        }
