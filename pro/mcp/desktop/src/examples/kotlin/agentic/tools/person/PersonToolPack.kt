package agentic.tools.person

import org.http4k.mcp.capability.CapabilityPack
import java.time.LocalDate
import java.time.YearMonth

fun PersonToolPack(name: String, appointments: (YearMonth) -> Map<LocalDate, List<String>>) = CapabilityPack(
    Diary(name, appointments),
)
