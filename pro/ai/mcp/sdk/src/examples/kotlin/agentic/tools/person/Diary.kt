package agentic.tools.person

import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.yearMonth
import org.http4k.ai.mcp.server.capability.ToolCapability
import org.http4k.routing.bind
import java.time.LocalDate
import java.time.YearMonth

fun Diary(name: String, appointments: (YearMonth) -> Map<LocalDate, List<String>>): ToolCapability {
    val arg = Tool.Arg.yearMonth().required("yearMonth", "year month in format yyyy-mm")
    return Tool(
        "diary_for_${name}",
        "details $name's diary appointments. Responds with a list of appointments for the given month. Any dates that are not listed have full availability.",
        arg,
    ) bind { yearMonth ->
        val content = appointments(arg(yearMonth))
            .flatMap { (date, slots) ->
                slots.map {
                    Content.Text("$date: $it")
                }
            }
        ToolResponse.Ok(content)
    }
}
