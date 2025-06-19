package experiment

import org.http4k.ai.model.Role.Companion.User
import org.http4k.ai.mcp.PromptResponse
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.Content.Text
import org.http4k.ai.mcp.model.Message
import org.http4k.ai.mcp.model.Prompt
import org.http4k.ai.mcp.model.PromptName
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.string
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.server.capability.PromptCapability
import org.http4k.ai.mcp.server.capability.ToolCapability
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.routing.bind
import org.http4k.routing.mcpWebsocket
import org.http4k.server.JettyLoom
import org.http4k.server.asServer

fun raiseClaim(): ToolCapability {
    val claimant = Tool.Arg.string().required("claimant")
    val item = Tool.Arg.string().required("item")
    val invoice = Tool.Arg.string().required("invoice")
    return Tool("raiseClaim", "Raise a claim for items", claimant, item, invoice) bind { req ->
        ToolResponse.Ok(Text("Claim raised ${claimant(req)} for item ${item(req)} invoice ${invoice(req)}"))
    }
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
                    User, Text(
                        """To raise a claim for ${claimant(req)} expensing ${item(req)}.
                - Obtain an invoice for the item.
                - Save the invoice to disk for future reference.
                - Raise a claim against us for the item and the cost, attaching the invoice.
                """.trimIndent()
                    )
                )
            )
        )
    }
}

val acmeHealthInsurance = mcpWebsocket(
    ServerMetaData("AcmeHealthInsurance", "1.0.0"),
    NoMcpSecurity,
    insuranceClaim(),
    raiseClaim()
).asServer(JettyLoom(9500)).start()
