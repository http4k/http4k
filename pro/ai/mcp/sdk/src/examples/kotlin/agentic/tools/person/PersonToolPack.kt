package agentic.tools.person

import org.http4k.ai.mcp.server.capability.CapabilityPack
import java.time.LocalDate
import java.time.YearMonth

fun PersonToolPack(name: String, appointments: (YearMonth) -> Map<LocalDate, List<String>>) = CapabilityPack(
    Diary(name, appointments),
)
