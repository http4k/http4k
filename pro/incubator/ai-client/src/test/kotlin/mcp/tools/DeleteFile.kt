package mcp.tools

import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.string
import org.http4k.ai.model.ToolName
import org.http4k.routing.bind
import java.io.File

object DeleteFile {
    val name = ToolName.of("delete_file")

    private val path = Tool.Arg.string().required("path")

    private val deleteFileTool = Tool(
        name.value,
        """
        Deletes the file(s) at the given path.
        :param path: The path to a file to delete.
    """.trimIndent(), path
    )

    operator fun invoke() = deleteFileTool bind {
        File(path(it)).deleteRecursively()
        ToolResponse.Ok("deleted")
    }
}
