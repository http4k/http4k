package mcp.tools

import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.util.McpJson.auto
import org.http4k.ai.model.ToolName
import org.http4k.lens.with
import org.http4k.routing.bind
import java.io.File

object RunCommand {

    class Input(val command: String, val path: String)
    class Output(val output: String, val exitCode: Int)

    val name = ToolName.of("run_command")

    private val input = Tool.Arg.auto<Input>(Input("ls", ".")).required("args")
    private val output = Tool.Output.auto<Output>(Output("filename.txt", 0)).toLens()

    private val runCommandTool = Tool(
        "run_command",
        """
        Run a shell command and return the output.
        :param command: The command to run.
        :param path: Where to run it
        :return: The output of the command and the exit code
    """, input, output = output
    )

    operator fun invoke() = runCommandTool bind {
        val args = input(it)
        val process = ProcessBuilder()
            .command(args.command)
            .directory(File(args.path))
            .start()
        ToolResponse.Ok().with(output of Output(process.inputReader().readText(), process.exitValue()))
    }
}
