package agentic.tools.person

import org.http4k.lens.localDate
import org.http4k.mcp.ToolResponse
import org.http4k.mcp.capability.ToolCapability
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.Tool
import org.http4k.routing.bind
import java.time.LocalDate

fun Diary(name: String, appointments: (LocalDate) -> List<Content.Text>): ToolCapability {
    val arg = Tool.Arg.localDate().required("date")
    return Tool(
        "diaryF$name", "details $name's diary appointments. Responds with a list of appointments for the given date",
        arg,
    ) bind {
        ToolResponse.Ok(appointments(arg(it)))
    }
}
