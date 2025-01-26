package agentic.tools.person

import org.http4k.mcp.capability.CapabilityPack
import org.http4k.mcp.model.Content
import java.time.LocalDate

fun PersonToolPack(name: String, appointments: (LocalDate) -> List<Content.Text>) = CapabilityPack(
    Diary(name, appointments),
)
