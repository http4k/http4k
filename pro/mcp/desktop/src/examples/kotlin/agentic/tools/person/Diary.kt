package agentic.tools.person

import org.http4k.lens.yearMonth
import org.http4k.mcp.ToolResponse
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.Tool
import org.http4k.mcp.server.capability.ToolCapability
import org.http4k.routing.bind
import java.time.LocalDate
import java.time.YearMonth

fun Diary(name: String, appointments: (YearMonth) -> Map<LocalDate, List<String>>): ToolCapability {
    val arg = Tool.Arg.yearMonth().required("yearMonth", "year month in format yyyy-mm")
    return Tool(
        "diary_for_${name}",
        "details $name's diary appointments. Responds with a list of appointments for the given month",
        arg,
    ) bind {
        val content = appointments(arg(it))
            .flatMap { (date, slots) ->
                slots.map {
                    Content.Text("$date: $it")
                }
            }
        ToolResponse.Ok(content)
    }
}
