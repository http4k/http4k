package mcp

import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.string
import org.http4k.ai.model.ToolName
import org.http4k.routing.bind
import java.io.File

object ListFiles {
    val name = ToolName.of("list_files")

    private val path = Tool.Arg.string().required("path")

    private val listFilesTool = Tool(
        name.value,
        """
        Lists the files in a directory provided by the user.
        :param path: The path to a directory to list files from.
        :return: A list of files in the directory.
    """.trimIndent(), path
    )

    operator fun invoke() = listFilesTool bind {
        val files = File(path(it)).listFiles()?.toList() ?: emptyList()
        ToolResponse.Ok(files.joinToString(", "))
    }
}
