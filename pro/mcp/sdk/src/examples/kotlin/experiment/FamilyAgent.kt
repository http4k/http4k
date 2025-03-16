package experiment

import org.http4k.lens.string
import org.http4k.mcp.CompletionRequest
import org.http4k.mcp.CompletionResponse
import org.http4k.mcp.ToolResponse
import org.http4k.mcp.model.Content.Text
import org.http4k.mcp.model.Reference
import org.http4k.mcp.model.Tool
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.server.capability.CompletionCapability
import org.http4k.mcp.server.capability.ToolCapability
import org.http4k.routing.bind
import org.http4k.routing.mcpSse
import org.http4k.server.Helidon
import org.http4k.server.asServer
import java.io.File


fun getFamilyMembers(): CompletionCapability = Reference.Prompt("Family Members") bind { req: CompletionRequest ->
    CompletionResponse(listOf("Alice", "Bob", "Charlie", "David"))
}

fun saveToMyDisk(): ToolCapability {
    val fileName = Tool.Arg.required("filename")
    val content = Tool.Arg.string().required("content")
    return Tool(
        "saveFile", "Save a file to my disk", fileName,
        content
    ) bind { req ->
        File(fileName(req)).writeText(content(req))
        ToolResponse.Ok(Text("File saved ${fileName(req)}"))
    }
}

val familyAgent = mcpSse(
    ServerMetaData("my family agent", "1.0.0"),
    getFamilyMembers(),
    saveToMyDisk()
).asServer(Helidon(7500)).start()
