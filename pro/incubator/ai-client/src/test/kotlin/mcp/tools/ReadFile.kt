package mcp.tools

import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.string
import org.http4k.ai.model.ToolName
import org.http4k.routing.bind
import java.io.File

object ReadFile {
    val name = ToolName.of("read_file")

    private val filename = Tool.Arg.string().required("filename")

    private val readFileTool = Tool(
        name.value,
        """
        Gets the full content of a file provided by the user.
        :param filename: The name of the file to read.
        :return: The full content of the file.
    """, filename
    )

    operator fun invoke() = readFileTool bind { ToolResponse.Ok(File(filename(it)).readText()) }
}

