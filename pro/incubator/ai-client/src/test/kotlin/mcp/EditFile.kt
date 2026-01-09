package mcp

import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.string
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.model.ToolName
import org.http4k.routing.bind
import java.io.File

object EditFile {
    val name = ToolName.of("edit_file")

    private val path = Tool.Arg.string().required("path")
    private val oldStr = Tool.Arg.string().required("old_str")
    private val newStr = Tool.Arg.string().required("new_str")

    private val editFileTool = Tool(
        name.value,
        """
        Replaces first occurrence of old_str with new_str in file. If old_str is empty,
        create/overwrite file with new_str.
        :param path: The path to the file to edit.
        :param old_str: The string to replace.
        :param new_str: The string to replace with.
        :return: A dictionary with the path to the file and the action taken.
    """.trimIndent(), oldStr, newStr, path
    )

    operator fun invoke() = editFileTool bind {
        val file = File(path(it))
        val toReplace = oldStr(it)
        val replacement = newStr(it)

        val action = if (toReplace.isEmpty()) {
            file.writeText(replacement, Charsets.UTF_8)
            "created"
        } else {
            val original = file.readText(Charsets.UTF_8)
            val index = original.indexOf(toReplace)
            when {
                index == -1 -> "old_str not found"

                else -> {
                    file.writeText(original.replace(toReplace, replacement), Charsets.UTF_8)
                    "edited"
                }
            }
        }

        ToolResponse.Ok(McpJson.asFormatString(mapOf("path" to file.absolutePath, "action" to action)))
    }
}
