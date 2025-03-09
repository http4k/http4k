package experiment

import org.http4k.connect.model.Base64Blob
import org.http4k.lens.value
import org.http4k.mcp.CompletionResponse
import org.http4k.mcp.PromptResponse
import org.http4k.mcp.ToolResponse
import org.http4k.mcp.model.Content.Text
import org.http4k.mcp.model.Message
import org.http4k.mcp.model.Prompt
import org.http4k.mcp.model.PromptName
import org.http4k.mcp.model.Reference
import org.http4k.mcp.model.Role.user
import org.http4k.mcp.model.Tool
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.server.capability.CompletionCapability
import org.http4k.mcp.server.capability.PromptCapability
import org.http4k.mcp.server.capability.ToolCapability
import org.http4k.routing.bind
import org.http4k.routing.mcpSse
import org.http4k.server.Helidon
import org.http4k.server.asServer
import java.io.File


fun getClaimants(): CompletionCapability = Reference.Prompt("HealthInsuranceClaim") bind { req ->
    CompletionResponse(listOf("Alice", "Bob", "Charlie", "David"))
}

fun insuranceClaim(): PromptCapability {
    val claimant = Prompt.Arg.required("claimant")
    val item = Prompt.Arg.required("item")
    return Prompt(
        PromptName.of("HealthInsuranceClaim"),
        "Raising a health insurance claim",
        claimant,
        item
    ) bind { req ->
        PromptResponse(
            listOf(
                Message(
                    user, Text(
                        """To raise a claim for ${claimant(req)} expensing ${item(req)}.
                - Check their purchases using the resource: purchases://${claimant(req)}.
                - Download the invoice for the purchase using the resource: purchases://invoices/${item(req)}.
                - Save the invoice to disk.
                - Raise a claim against AcmeHealthInsurance for the item and the cost, attaching the invoice.
                """.trimIndent()
                    )
                )
            )
        )
    }
}

fun saveToMyDisk(): ToolCapability {
    val fileName = Tool.Arg.required("filename")
    val content = Tool.Arg.value(Base64Blob).required("content")
    return Tool(
        "saveFile", "Save a file to my disk", fileName,
        content
    ) bind { req ->
        File(fileName(req)).writeBytes(content(req).decodedBytes())
        ToolResponse.Ok(Text("File saved ${fileName(req)}"))
    }
}

val family = mcpSse(
    ServerMetaData("my family agent", "1.0.0"),
    getClaimants(),
    insuranceClaim(),
    saveToMyDisk()
).asServer(Helidon(7500)).start()
